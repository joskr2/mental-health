package com.clinica.mentalhealth.service;

import com.clinica.mentalhealth.config.SecurityProperties;
import com.clinica.mentalhealth.domain.RefreshTokenSession;
import com.clinica.mentalhealth.domain.User;
import com.clinica.mentalhealth.repository.RefreshTokenSessionRepository;
import com.clinica.mentalhealth.repository.UserRepository;
import com.clinica.mentalhealth.security.JwtService;
import com.clinica.mentalhealth.web.dto.LoginResponse;
import com.clinica.mentalhealth.web.dto.SessionInfo;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Servicio para gestionar sesiones de refresh tokens con estado.
 * Implementa:
 * - Tokens de un solo uso (one-time use)
 * - Detección de reutilización de tokens (posible robo)
 * - Cierre de sesión en todos los dispositivos
 * - Rotación segura de tokens
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenSessionService {

  private final RefreshTokenSessionRepository sessionRepository;
  private final UserRepository userRepository;
  private final JwtService jwtService;
  private final SecurityProperties securityProperties;

  // Tiempo de retención de tokens expirados para auditoría
  private static final Duration EXPIRED_TOKEN_RETENTION = Duration.ofDays(7);

  /**
   * Crea una nueva sesión de refresh token para el usuario.
   * Si el usuario excede el límite de sesiones, revoca la más antigua.
   */
  @Transactional
  public Mono<LoginResponse> createSession(User user, String deviceInfo, String ipAddress) {
    String accessToken = jwtService.generateAccessToken(user);
    String refreshToken = jwtService.generateRefreshToken(user);
    Claims claims = jwtService.getRefreshClaims(refreshToken);

    String tokenId = claims.get("jti", String.class);
    Instant expiresAt = claims.getExpiration().toInstant();

    RefreshTokenSession session = RefreshTokenSession.create(
        user.id(), tokenId, deviceInfo, ipAddress, expiresAt);

    return enforceSessionLimit(Objects.requireNonNull(user.id()))
        .then(sessionRepository.save(Objects.requireNonNull(session)))
        .doOnSuccess(s -> log.info("Nueva sesión creada para usuario {} desde {}", user.getUsername(), ipAddress))
        .map(saved -> new LoginResponse(accessToken, refreshToken));
  }

  /**
   * Rota el refresh token de forma segura.
   * - Invalida el token actual
   * - Genera un nuevo token
   * - Detecta reutilización de tokens revocados
   */
  @Transactional
  public Mono<LoginResponse> rotateToken(String refreshToken, String deviceInfo, String ipAddress) {
    if (!jwtService.validateRefreshToken(refreshToken)) {
      log.warn("Intento de rotación con token inválido desde {}", ipAddress);
      return Mono.empty();
    }

    Claims claims = jwtService.getRefreshClaims(refreshToken);
    String tokenId = claims.get("jti", String.class);
    Long userId = Long.valueOf(claims.get("userId").toString());

    return sessionRepository.findByTokenId(tokenId)
        .flatMap(session -> {
          // Detectar reutilización de token revocado (posible robo)
          if (session.revoked()) {
            log.error("⚠️ ALERTA DE SEGURIDAD: Reutilización de token revocado detectada! " +
                "Usuario: {}, Token: {}, IP: {}. Revocando TODAS las sesiones.",
                userId, tokenId, ipAddress);
            // Revocar TODAS las sesiones del usuario (medida de seguridad)
            return sessionRepository.revokeAllByUserId(userId)
                .then(Mono.empty());
          }

          // Token válido: crear nuevo y revocar el actual
          return userRepository.findById(Objects.requireNonNull(userId))
              .flatMap(user -> {
                String newAccessToken = jwtService.generateAccessToken(user);
                String newRefreshToken = jwtService.generateRefreshToken(user);
                Claims newClaims = jwtService.getRefreshClaims(newRefreshToken);
                String newTokenId = newClaims.get("jti", String.class);
                Instant newExpiresAt = newClaims.getExpiration().toInstant();

                // Revocar token actual y marcar con el nuevo
                RefreshTokenSession revokedSession = session.revokeAndReplace(newTokenId);

                // Crear nueva sesión
                RefreshTokenSession newSession = RefreshTokenSession.create(
                    userId, newTokenId, deviceInfo, ipAddress, newExpiresAt);

                return sessionRepository.save(Objects.requireNonNull(revokedSession))
                    .then(sessionRepository.save(Objects.requireNonNull(newSession)))
                    .doOnSuccess(s -> log.debug("Token rotado para usuario {} desde {}",
                        user.getUsername(), ipAddress))
                    .map(saved -> new LoginResponse(newAccessToken, newRefreshToken));
              });
        })
        .switchIfEmpty(Mono.defer(() -> {
          // Token no encontrado en BD (posiblemente muy antiguo o manipulado)
          log.warn("Token no encontrado en BD: {} desde IP: {}", tokenId, ipAddress);
          return Mono.empty();
        }));
  }

  /**
   * Revoca un token específico (logout de un dispositivo)
   */
  @Transactional
  public Mono<Boolean> revokeToken(String refreshToken) {
    if (!jwtService.validateRefreshToken(refreshToken)) {
      return Mono.just(false);
    }

    Claims claims = jwtService.getRefreshClaims(refreshToken);
    String tokenId = claims.get("jti", String.class);

    return sessionRepository.revokeByTokenId(tokenId)
        .map(count -> count > 0)
        .doOnSuccess(wasRevoked -> {
          if (Boolean.TRUE.equals(wasRevoked)) {
            log.info("Token revocado: {}", tokenId);
          }
        });
  }

  /**
   * Revoca todas las sesiones de un usuario (logout de todos los dispositivos)
   */
  @Transactional
  public Mono<Integer> revokeAllUserSessions(Long userId) {
    return sessionRepository.revokeAllByUserId(userId)
        .doOnSuccess(count -> log.info("Revocadas {} sesiones del usuario {}", count, userId));
  }

  /**
   * Obtiene todas las sesiones activas de un usuario
   */
  public Flux<SessionInfo> getActiveSessions(Long userId) {
    return sessionRepository.findActiveSessionsByUserId(userId)
        .map(session -> new SessionInfo(
            session.tokenId(),
            session.deviceInfo(),
            session.ipAddress(),
            session.createdAt()));
  }

  /**
   * Verifica si un token está activo (para uso en filtros de seguridad)
   */
  public Mono<Boolean> isTokenActive(String tokenId) {
    return sessionRepository.existsActiveToken(tokenId);
  }

  /**
   * Limpia tokens expirados periódicamente (cada día a las 3 AM)
   */
  @Scheduled(cron = "0 0 3 * * *")
  @Transactional
  public void cleanupExpiredTokens() {
    Instant threshold = Instant.now().minus(EXPIRED_TOKEN_RETENTION);
    sessionRepository.deleteExpiredTokens(threshold)
        .doOnSuccess(count -> log.info("Limpieza de tokens: {} tokens expirados eliminados", count))
        .subscribe();
  }

  /**
   * Aplica el límite de sesiones activas por usuario
   */
  private Mono<Void> enforceSessionLimit(Long userId) {
    int maxSessions = securityProperties.maxSessions();
    if (maxSessions <= 0) {
      return Mono.empty();
    }

    return sessionRepository.countActiveSessionsByUserId(Objects.requireNonNull(userId))
        .flatMap(count -> {
          if (count >= maxSessions) {
            // Revocar la sesión más antigua
            return sessionRepository.findActiveSessionsByUserId(userId)
                .sort((s1, s2) -> s1.createdAt().compareTo(s2.createdAt()))
                .next()
                .flatMap(oldestSession -> sessionRepository.save(Objects.requireNonNull(oldestSession.revoke())))
                .then();
          }
          return Mono.empty();
        });
  }
}
