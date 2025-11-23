package com.clinica.mentalhealth.repository;

import com.clinica.mentalhealth.domain.Room;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface RoomRepository extends ReactiveCrudRepository<Room, Long> {
}