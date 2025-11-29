package com.clinica.mentalhealth.service;

import com.clinica.mentalhealth.config.ReactiveCache;
import com.clinica.mentalhealth.domain.Room;
import com.clinica.mentalhealth.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private static final String ROOM_NOT_FOUND = "Sala no encontrada";
    private static final String ROOM_ID_REQUIRED = "ID de sala requerido";
    private static final String CACHE_PREFIX = "room:";
    private static final String CACHE_ALL = CACHE_PREFIX + "all";

    private final RoomRepository roomRepository;
    private final ReactiveCache reactiveCache;

    // --- LECTURA ---

    /**
     * Retorna todas las salas como Flux (streaming, no cacheado).
     */
    public Flux<Room> findAll() {
        log.debug("Buscando todas las salas (stream)");
        return roomRepository.findAll();
    }

    /**
     * Versión cacheada reactiva que retorna Mono<List>.
     * Usa ReactiveCache para caché no bloqueante.
     */
    public Mono<List<Room>> findAllCached() {
        return reactiveCache.cacheList(CACHE_ALL, () -> {
            log.debug("Cache miss: buscando todas las salas de BD");
            return roomRepository.findAll().collectList();
        });
    }

    /**
     * Busca una sala por ID con caché reactivo.
     */
    public Mono<Room> findById(@NonNull Long id) {
        String cacheKey = CACHE_PREFIX + id;
        return reactiveCache.cacheMono(cacheKey, () -> {
            log.debug("Cache miss: buscando sala con ID: {}", id);
            return roomRepository.findById(Objects.requireNonNull(id, ROOM_ID_REQUIRED))
                    .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, ROOM_NOT_FOUND)));
        });
    }

    // --- CREACIÓN ---

    @Transactional
    public Mono<Room> createRoom(String name) {
        log.info("Creando sala: {}", name);
        return roomRepository.save(new Room(null, name))
                // Invalidar caché después de crear
                .flatMap(room -> reactiveCache.evictByPrefix(CACHE_PREFIX).thenReturn(room));
    }

    // --- ACTUALIZACIÓN ---

    @Transactional
    public Mono<Room> updateRoom(@NonNull Long id, String name) {
        log.info("Actualizando sala con ID: {}", id);
        return roomRepository.findById(Objects.requireNonNull(id, ROOM_ID_REQUIRED))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, ROOM_NOT_FOUND)))
                .flatMap(existing -> roomRepository.save(new Room(existing.id(), name)))
                // Invalidar caché después de actualizar
                .flatMap(room -> reactiveCache.evictByPrefix(CACHE_PREFIX).thenReturn(room));
    }

    // --- ELIMINACIÓN ---

    @Transactional
    public Mono<Void> deleteRoom(@NonNull Long id) {
        log.info("Eliminando sala con ID: {}", id);
        return roomRepository.findById(Objects.requireNonNull(id, ROOM_ID_REQUIRED))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, ROOM_NOT_FOUND)))
                .flatMap(roomRepository::delete)
                // Invalidar caché después de eliminar
                .then(reactiveCache.evictByPrefix(CACHE_PREFIX));
    }
}
