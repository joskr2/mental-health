package com.clinica.mentalhealth.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * Caché reactivo usando Caffeine con wrapping no bloqueante.
 * 
 * Diseñado para trabajar con Mono/Flux sin bloquear el event loop.
 * Usa Mono.defer() para evaluación lazy y Mono.fromCallable() para
 * operaciones de lectura del caché.
 * 
 * Uso:
 * 
 * <pre>
 * reactiveCache.cacheMono("psychologists:all",
 *     () -> repository.findAll().collectList());
 * </pre>
 */
@Component
public class ReactiveCache {

  private final Cache<String, Object> caffeineCache;

  public ReactiveCache() {
    this.caffeineCache = Caffeine.newBuilder()
        .initialCapacity(100)
        .maximumSize(500)
        .expireAfterWrite(Duration.ofMinutes(30))
        .recordStats()
        .build();
  }

  /**
   * Cachea un Mono de forma reactiva.
   * Si el valor está en caché, lo retorna inmediatamente.
   * Si no, ejecuta el supplier y guarda el resultado.
   *
   * @param key      Clave del caché
   * @param supplier Proveedor del valor si no está en caché
   * @param <T>      Tipo del valor
   * @return Mono con el valor (cacheado o recién obtenido)
   */
  @SuppressWarnings("unchecked")
  public <T> Mono<T> cacheMono(String key, Supplier<Mono<T>> supplier) {
    return Mono.defer(() -> {
      // Intenta obtener del caché (operación rápida, no bloquea)
      T cached = (T) caffeineCache.getIfPresent(key);
      if (cached != null) {
        return Mono.just(cached);
      }

      // Cache miss: ejecutar supplier y guardar resultado
      return supplier.get()
          .doOnNext(value -> caffeineCache.put(key, value));
    });
  }

  /**
   * Cachea una lista de forma reactiva.
   * Convierte internamente Flux a List para almacenamiento eficiente.
   *
   * @param key      Clave del caché
   * @param supplier Proveedor de Mono<List<T>>
   * @param <T>      Tipo de elementos en la lista
   * @return Mono con la lista
   */
  public <T> Mono<List<T>> cacheList(String key, Supplier<Mono<List<T>>> supplier) {
    return cacheMono(key, supplier);
  }

  /**
   * Invalida una entrada específica del caché.
   * Retorna Mono<Void> para composición reactiva.
   */
  public Mono<Void> evict(String key) {
    return Mono.fromRunnable(() -> caffeineCache.invalidate(key));
  }

  /**
   * Invalida todas las entradas que comienzan con el prefijo dado.
   */
  public Mono<Void> evictByPrefix(String prefix) {
    return Mono.fromRunnable(() -> caffeineCache.asMap().keySet().stream()
        .filter(key -> key.startsWith(prefix))
        .forEach(caffeineCache::invalidate));
  }

  /**
   * Invalida todas las entradas del caché.
   */
  public Mono<Void> evictAll() {
    return Mono.fromRunnable(caffeineCache::invalidateAll);
  }

  /**
   * Obtiene estadísticas del caché para monitoreo.
   */
  public String getStats() {
    var stats = caffeineCache.stats();
    return String.format(
        "Hits: %d, Misses: %d, Hit Rate: %.2f%%, Evictions: %d, Size: %d",
        stats.hitCount(),
        stats.missCount(),
        stats.hitRate() * 100,
        stats.evictionCount(),
        caffeineCache.estimatedSize());
  }
}
