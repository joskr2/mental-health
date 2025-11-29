package com.clinica.mentalhealth.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Configuración de caché con Caffeine para WebFlux.
 * 
 * IMPORTANTE: Spring Cache estándar NO es reactivo. Las anotaciones @Cacheable
 * en métodos que retornan Flux/Mono tienen comportamiento impredecible.
 * 
 * Estrategia adoptada:
 * 1. Métodos que retornan Mono<T> -> Pueden usar @Cacheable (se cachea el Mono
 * materializado)
 * 2. Métodos que retornan Flux<T> -> Deben usar caché manual con
 * CacheMono/CacheFlux
 * o convertir a Mono<List<T>> para ser cacheables.
 * 
 * Esta configuración proporciona el CacheManager base. Los servicios que
 * necesitan
 * cachear Flux deben usar ReactiveCache o convertir a Mono<List<T>>.
 */
@Configuration
public class CacheConfig {

  @Bean
  public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();
    cacheManager.setCaffeine(Objects.requireNonNull(caffeineCacheBuilder(), "Caffeine builder cannot be null"));
    // Registrar caches específicos
    cacheManager.setCacheNames(List.of(
        "psychologists",
        "psychologistsList", // Para Mono<List<Psychologist>>
        "rooms",
        "roomsList" // Para Mono<List<Room>>
    ));
    return cacheManager;
  }

  private Caffeine<Object, Object> caffeineCacheBuilder() {
    return Caffeine.newBuilder()
        .initialCapacity(100)
        .maximumSize(500)
        .expireAfterWrite(Duration.ofMinutes(30))
        .recordStats(); // Habilitar estadísticas para monitoreo
  }
}
