package com.clinica.mentalhealth.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Configuración de Rate Limiting para proteger la API contra abuso.
 *
 * Implementa límites diferenciados por tipo de endpoint:
 * - Autenticación: 10 requests/minuto (prevenir fuerza bruta)
 * - IA/Chat: 20 requests/minuto (prevenir abuso de API externa)
 * - General: 100 requests/minuto (uso normal)
 *
 * Los límites se aplican por IP del cliente.
 */
@Slf4j
@Configuration
public class RateLimitConfig {

  // Almacén de buckets por IP (en producción usar Redis para clusters)
  private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();
  private final Map<String, Bucket> aiBuckets = new ConcurrentHashMap<>();
  private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();

  // Configuración de límites
  private static final int AUTH_LIMIT = 10; // requests por minuto
  private static final int AI_LIMIT = 20; // requests por minuto
  private static final int GENERAL_LIMIT = 100; // requests por minuto
  private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);

  // Headers estándar para rate limiting
  private static final String HEADER_LIMIT = "X-RateLimit-Limit";
  private static final String HEADER_REMAINING = "X-RateLimit-Remaining";
  private static final String HEADER_RETRY_AFTER = "Retry-After";

  @Bean
  public WebFilter rateLimitFilter() {
    return new RateLimitWebFilter();
  }

  private class RateLimitWebFilter implements WebFilter {

    @Override
    @NonNull
    public Mono<Void> filter(
      @NonNull ServerWebExchange exchange,
      @NonNull WebFilterChain chain
    ) {
      String path = exchange.getRequest().getPath().value();
      String clientIp = extractClientIp(exchange);

      // Endpoints públicos sin rate limiting
      if (isPublicStaticEndpoint(path)) {
        return chain.filter(exchange);
      }

      // Seleccionar bucket según el tipo de endpoint
      RateLimitContext context = selectBucketContext(path, clientIp);

      // Intentar consumir un token
      if (context.bucket.tryConsume(1)) {
        // Añadir headers informativos
        long remaining = context.bucket.getAvailableTokens();
        exchange
          .getResponse()
          .getHeaders()
          .add(HEADER_LIMIT, String.valueOf(context.limit));
        exchange
          .getResponse()
          .getHeaders()
          .add(HEADER_REMAINING, String.valueOf(remaining));
        return chain.filter(exchange);
      }

      // Rate limit excedido
      log.warn(
        "Rate limit excedido para IP {} en endpoint {} (límite: {}/min)",
        clientIp,
        path,
        context.limit
      );

      exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
      exchange
        .getResponse()
        .getHeaders()
        .add(HEADER_LIMIT, String.valueOf(context.limit));
      exchange.getResponse().getHeaders().add(HEADER_REMAINING, "0");
      exchange.getResponse().getHeaders().add(HEADER_RETRY_AFTER, "60");

      return exchange.getResponse().setComplete();
    }

    private boolean isPublicStaticEndpoint(String path) {
      return (
        path.startsWith("/docs") ||
        path.startsWith("/scalar") ||
        path.startsWith("/v3/api-docs") ||
        path.startsWith("/actuator/health") ||
        path.startsWith("/webjars") ||
        path.startsWith("/static")
      );
    }

    private RateLimitContext selectBucketContext(String path, String clientIp) {
      if (path.startsWith("/api/auth")) {
        return new RateLimitContext(
          authBuckets.computeIfAbsent(clientIp, k -> createBucket(AUTH_LIMIT)),
          AUTH_LIMIT
        );
      }

      if (path.startsWith("/api/agent")) {
        return new RateLimitContext(
          aiBuckets.computeIfAbsent(clientIp, k -> createBucket(AI_LIMIT)),
          AI_LIMIT
        );
      }

      return new RateLimitContext(
        generalBuckets.computeIfAbsent(clientIp, k ->
          createBucket(GENERAL_LIMIT)
        ),
        GENERAL_LIMIT
      );
    }

    private Bucket createBucket(int limit) {
      Bandwidth bandwidth = Bandwidth.builder()
              .capacity(limit)
              .refillGreedy(limit, REFILL_PERIOD)
              .build();
      return Bucket.builder().addLimit(bandwidth).build();
    }

    private String extractClientIp(ServerWebExchange exchange) {
      // Considerar proxies (X-Forwarded-For)
      String xForwardedFor = exchange
        .getRequest()
        .getHeaders()
        .getFirst("X-Forwarded-For");
      if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
        return xForwardedFor.split(",")[0].trim();
      }

      var remoteAddress = exchange.getRequest().getRemoteAddress();
      if (remoteAddress != null) {
        return remoteAddress.getAddress().getHostAddress();
      }

      return "unknown";
    }
  }

  /**
   * Contexto de rate limiting con bucket y límite asociado.
   */
  private record RateLimitContext(Bucket bucket, int limit) {}

  /**
   * Limpieza periódica de buckets inactivos (para evitar memory leaks).
   * En producción con Redis esto no es necesario.
   */
  @org.springframework.scheduling.annotation.Scheduled(fixedRate = 300000) // 5 minutos
  public void cleanupInactiveBuckets() {
    int totalCleaned = 0;

    // Limpiar buckets con todos los tokens disponibles (inactivos)
    totalCleaned += cleanupBucketMap(authBuckets, AUTH_LIMIT);
    totalCleaned += cleanupBucketMap(aiBuckets, AI_LIMIT);
    totalCleaned += cleanupBucketMap(generalBuckets, GENERAL_LIMIT);

    if (totalCleaned > 0) {
      log.debug(
        "Limpieza de rate limiters: {} buckets inactivos eliminados",
        totalCleaned
      );
    }
  }

  private int cleanupBucketMap(Map<String, Bucket> bucketMap, int limit) {
    int cleaned = 0;
    var iterator = bucketMap.entrySet().iterator();
    while (iterator.hasNext()) {
      var entry = iterator.next();
      // Si tiene todos los tokens, probablemente está inactivo
      if (entry.getValue().getAvailableTokens() >= limit) {
        iterator.remove();
        cleaned++;
      }
    }
    return cleaned;
  }
}
