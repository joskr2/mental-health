package com.clinica.mentalhealth.security;

import com.clinica.mentalhealth.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Servicio para generación y validación de tokens JWT.
 *
 * Las claves secretas se cargan desde configuración externa (variables de entorno
 * o application.properties) en lugar de generarse en runtime. Esto permite:
 * - Persistencia de tokens entre reinicios de la aplicación
 * - Consistencia en clusters/múltiples instancias
 * - Gestión segura de secretos vía vault/secrets manager
 *
 * IMPORTANTE: En producción, usar secretos de al menos 256 bits (32 caracteres)
 * y almacenarlos de forma segura (AWS Secrets Manager, Vault, etc.)
 */
@Slf4j
@Service
public class JwtService {

  // Constants to avoid duplicated literals
  private static final String CLAIM_USER_ID = "userId";
  private static final String CLAIM_TYPE = "type";
  private static final String CLAIM_TYPE_ACCESS = "access";
  private static final String CLAIM_TYPE_REFRESH = "refresh";
  private static final String CLAIM_JTI = "jti";

  // Claves desde configuración (con valores por defecto solo para desarrollo)
  @Value("${jwt.access-secret:development-access-secret-key-min-32-chars!!}")
  private String accessSecretString;

  @Value("${jwt.refresh-secret:development-refresh-secret-key-min-32-chars!}")
  private String refreshSecretString;

  @Value("${jwt.access-expiration:PT30M}") // 30 minutos en formato ISO-8601
  private Duration accessTtl;

  @Value("${jwt.refresh-expiration:P14D}") // 14 días en formato ISO-8601
  private Duration refreshTtl;

  // Claves derivadas de los strings de configuración
  private SecretKey accessKey;
  private SecretKey refreshKey;

  @PostConstruct
  public void init() {
    // Validar longitud mínima de secretos
    validateSecretLength(accessSecretString, "jwt.access-secret");
    validateSecretLength(refreshSecretString, "jwt.refresh-secret");

    // Derivar claves HMAC-SHA256 desde los strings
    this.accessKey = Keys.hmacShaKeyFor(
      accessSecretString.getBytes(StandardCharsets.UTF_8)
    );
    this.refreshKey = Keys.hmacShaKeyFor(
      refreshSecretString.getBytes(StandardCharsets.UTF_8)
    );

    log.info(
      "JwtService inicializado. Access TTL: {}, Refresh TTL: {}",
      accessTtl,
      refreshTtl
    );

    // Advertir si se usan valores por defecto (solo desarrollo)
    if (accessSecretString.contains("development")) {
      log.warn("⚠️ USANDO CLAVES JWT POR DEFECTO - NO USAR EN PRODUCCIÓN");
    }
  }

  private void validateSecretLength(String secret, String propertyName) {
    if (secret == null || secret.length() < 32) {
      throw new IllegalStateException(
        String.format(
          "La propiedad '%s' debe tener al menos 32 caracteres para seguridad HMAC-SHA256",
          propertyName
        )
      );
    }
  }

  /**
   * Genera un token de acceso (corta duración) para el usuario.
   */
  public String generateAccessToken(User user) {
    long now = System.currentTimeMillis();
    return Jwts.builder()
      .subject(user.getUsername())
      .claim(CLAIM_TYPE, CLAIM_TYPE_ACCESS)
      .claim("role", user.role().name())
      .claim(CLAIM_USER_ID, user.id())
      .issuedAt(new Date(now))
      .expiration(new Date(now + accessTtl.toMillis()))
      .signWith(accessKey)
      .compact();
  }

  /**
   * Genera un refresh token (larga duración) para el usuario.
   * Incluye un ID único (jti) para tracking de sesiones.
   */
  public String generateRefreshToken(User user) {
    long now = System.currentTimeMillis();
    return Jwts.builder()
      .subject(user.getUsername())
      .claim(CLAIM_TYPE, CLAIM_TYPE_REFRESH)
      .claim(CLAIM_USER_ID, user.id())
      .claim(CLAIM_JTI, UUID.randomUUID().toString())
      .issuedAt(new Date(now))
      .expiration(new Date(now + refreshTtl.toMillis()))
      .signWith(refreshKey)
      .compact();
  }

  /**
   * Legacy method para compatibilidad.
   * @deprecated Usar generateAccessToken en su lugar.
   */
  @Deprecated(forRemoval = true)
  public String generateToken(User user) {
    return generateAccessToken(user);
  }

  /**
   * Valida un access token.
   * @return true si el token es válido y es de tipo "access"
   */
  public boolean validateAccessToken(String token) {
    try {
      Claims claims = parseClaims(token, accessKey);
      String type = claims.get(CLAIM_TYPE, String.class);
      return CLAIM_TYPE_ACCESS.equals(type) || type == null; // null para retrocompatibilidad
    } catch (Exception e) {
      log.debug("Token de acceso inválido: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Valida un refresh token.
   * @return true si el token es válido y es de tipo "refresh"
   */
  public boolean validateRefreshToken(String token) {
    try {
      Claims claims = parseClaims(token, refreshKey);
      return CLAIM_TYPE_REFRESH.equals(claims.get(CLAIM_TYPE));
    } catch (Exception e) {
      log.debug("Refresh token inválido: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Obtiene los claims de un access token válido.
   */
  public Claims getAccessClaims(String token) {
    return parseClaims(token, accessKey);
  }

  /**
   * Obtiene los claims de un refresh token válido.
   */
  public Claims getRefreshClaims(String token) {
    return parseClaims(token, refreshKey);
  }

  /**
   * Rota un refresh token generando uno nuevo con el mismo usuario.
   * El nuevo token tendrá un nuevo jti y nueva fecha de expiración.
   */
  public String rotateRefreshToken(String refreshToken) {
    Claims claims = getRefreshClaims(refreshToken);
    long now = System.currentTimeMillis();
    return Jwts.builder()
      .subject(claims.getSubject())
      .claim(CLAIM_TYPE, CLAIM_TYPE_REFRESH)
      .claim(CLAIM_USER_ID, claims.get(CLAIM_USER_ID))
      .claim(CLAIM_JTI, UUID.randomUUID().toString())
      .issuedAt(new Date(now))
      .expiration(new Date(now + refreshTtl.toMillis()))
      .signWith(refreshKey)
      .compact();
  }

  /**
   * Obtiene el tiempo de expiración del refresh token (para almacenamiento en BD).
   */
  public Duration getRefreshTokenTtl() {
    return refreshTtl;
  }

  /**
   * Parsea y valida un token JWT con la clave especificada.
   */
  private Claims parseClaims(String token, SecretKey key) {
    return Jwts.parser()
      .verifyWith(key)
      .build()
      .parseSignedClaims(token)
      .getPayload();
  }
}
