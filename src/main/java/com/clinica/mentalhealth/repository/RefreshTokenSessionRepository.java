package com.clinica.mentalhealth.repository;

import com.clinica.mentalhealth.domain.RefreshTokenSession;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Repositorio para gestionar sesiones de refresh tokens.
 * Permite operaciones CRUD y consultas especializadas para
 * validación, revocación y limpieza de tokens.
 */
public interface RefreshTokenSessionRepository extends ReactiveCrudRepository<RefreshTokenSession, Long> {

  /**
   * Busca una sesión por su token ID (jti claim del JWT)
   */
  Mono<RefreshTokenSession> findByTokenId(String tokenId);

  /**
   * Busca todas las sesiones activas de un usuario
   */
  @Query("SELECT * FROM refresh_token_sessions WHERE user_id = :userId AND revoked = false AND expires_at > NOW()")
  Flux<RefreshTokenSession> findActiveSessionsByUserId(Long userId);

  /**
   * Busca todas las sesiones (activas e inactivas) de un usuario
   */
  Flux<RefreshTokenSession> findByUserId(Long userId);

  /**
   * Revoca todas las sesiones de un usuario (logout de todos los dispositivos)
   */
  @Modifying
  @Query("UPDATE refresh_token_sessions SET revoked = true, revoked_at = NOW() WHERE user_id = :userId AND revoked = false")
  Mono<Integer> revokeAllByUserId(Long userId);

  /**
   * Revoca una sesión específica por token ID
   */
  @Modifying
  @Query("UPDATE refresh_token_sessions SET revoked = true, revoked_at = NOW() WHERE token_id = :tokenId AND revoked = false")
  Mono<Integer> revokeByTokenId(String tokenId);

  /**
   * Cuenta sesiones activas de un usuario
   */
  @Query("SELECT COUNT(*) FROM refresh_token_sessions WHERE user_id = :userId AND revoked = false AND expires_at > NOW()")
  Mono<Long> countActiveSessionsByUserId(Long userId);

  /**
   * Elimina tokens expirados (limpieza periódica)
   */
  @Modifying
  @Query("DELETE FROM refresh_token_sessions WHERE expires_at < :threshold")
  Mono<Integer> deleteExpiredTokens(Instant threshold);

  /**
   * Verifica si existe un token activo (no revocado y no expirado)
   */
  @Query("SELECT EXISTS(SELECT 1 FROM refresh_token_sessions WHERE token_id = :tokenId AND revoked = false AND expires_at > NOW())")
  Mono<Boolean> existsActiveToken(String tokenId);
}
