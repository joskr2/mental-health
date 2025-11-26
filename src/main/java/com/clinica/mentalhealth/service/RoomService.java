package com.clinica.mentalhealth.service;

import com.clinica.mentalhealth.domain.Room;
import com.clinica.mentalhealth.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    @Cacheable(value = "rooms", key = "'all'")
    public Flux<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    @Cacheable(value = "rooms", key = "#id")
    public Mono<Room> getRoomById(Long id) {
        return roomRepository.findById(id);
    }

    @CacheEvict(value = "rooms", allEntries = true)
    public Mono<Room> createRoom(Room room) {
        return roomRepository.save(room);
    }

    @CacheEvict(value = "rooms", allEntries = true)
    public Mono<Room> updateRoom(Room room) {
        return roomRepository.save(room);
    }

    @CacheEvict(value = "rooms", allEntries = true)
    public Mono<Void> deleteRoom(Long id) {
        return roomRepository.deleteById(id);
    }
}

