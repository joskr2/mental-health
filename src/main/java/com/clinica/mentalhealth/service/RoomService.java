package com.clinica.mentalhealth.service;

import com.clinica.mentalhealth.domain.Room;
import com.clinica.mentalhealth.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RoomService {

    private static final String ROOM_NOT_FOUND = "Sala no encontrada";
    private static final String ROOM_ID_REQUIRED = "ID de sala requerido";

    private final RoomRepository roomRepository;

    @Cacheable(value = "rooms", key = "'all'")
    public Flux<Room> findAll() {
        return roomRepository.findAll();
    }

    @Cacheable(value = "rooms", key = "#id")
    public Mono<Room> findById(Long id) {
        return roomRepository.findById(Objects.requireNonNull(id, ROOM_ID_REQUIRED))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, ROOM_NOT_FOUND)));
    }

    @Transactional
    @CacheEvict(value = "rooms", allEntries = true)
    public Mono<Room> createRoom(String name) {
        return roomRepository.save(new Room(null, name));
    }

    @Transactional
    @CacheEvict(value = "rooms", allEntries = true)
    public Mono<Room> updateRoom(Long id, String name) {
        return roomRepository.findById(Objects.requireNonNull(id, ROOM_ID_REQUIRED))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, ROOM_NOT_FOUND)))
                .flatMap(existing -> roomRepository.save(new Room(existing.id(), name)));
    }

    @Transactional
    @CacheEvict(value = "rooms", allEntries = true)
    public Mono<Void> deleteRoom(Long id) {
        return roomRepository.findById(Objects.requireNonNull(id, ROOM_ID_REQUIRED))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, ROOM_NOT_FOUND)))
                .flatMap(roomRepository::delete);
    }
}
