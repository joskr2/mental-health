package com.clinica.mentalhealth.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Entidad que representa una sesión de refresh token.
 * Permite:
 * - Invalidar tokens usados (one-time use)
 * - Detectar reutilización de tokens (posible robo)
 * - Cerrar sesión en todos los dispositivos
 * - Revocar tokens específicos
 */
@Table("refresh_token_sessions")
public record RefreshTokenSession(
    @Id Long id,

    @Column("user_id") Long userId,

    @Column("token_id") String tokenId, // UUID único del token (jti claim)

    @Column("device_info") String deviceInfo, // User-Agent o identificador del dispositivo

    @Column("ip_address") String ipAddress,

    @Column("created_at") Instant createdAt,

    @Column("expires_at") Instant expiresAt,

    @Column("revoked") boolean revoked,

    @Column("revoked_at") Instant revokedAt,

    @Column("replaced_by_token_id") String replacedByTokenId // Para detectar reutilización (token chain)
) {

  /**
   * Crea una nueva sesión de token activa
   */
  public static RefreshTokenSession create(Long userId, String tokenId, String deviceInfo,
      String ipAddress, Instant expiresAt) {
    return new RefreshTokenSession(
        null, // id generado por BD
        userId,
        tokenId,
        deviceInfo,
        ipAddress,
        Instant.now(), // createdAt
        expiresAt,
        false, // no revocado
        null, // revokedAt
        null // replacedByTokenId
    );
  }

  /**
   * Marca este token como revocado y reemplazado por otro
   */
  public RefreshTokenSession revokeAndReplace(String newTokenId) {
    return new RefreshTokenSession(
        this.id,
        this.userId,
        this.tokenId,
        this.deviceInfo,
        this.ipAddress,
        this.createdAt,
        this.expiresAt,
        true, // revocado
        Instant.now(), // momento de revocación
        newTokenId // token que lo reemplaza
    );
  }

  /**
   * Marca este token como revocado (sin reemplazo, ej: logout)
   */
  public RefreshTokenSession revoke() {
    return new RefreshTokenSession(
        this.id,
        this.userId,
        this.tokenId,
        this.deviceInfo,
        this.ipAddress,
        this.createdAt,
        this.expiresAt,
        true,
        Instant.now(),
        null);
  }

  /**
   * Verifica si el token está activo (no revocado y no expirado)
   */
  public boolean isActive() {
    return !revoked && Instant.now().isBefore(expiresAt);
  }
}
