package com.clinica.mentalhealth.web;

import com.clinica.mentalhealth.domain.Room;
import com.clinica.mentalhealth.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    // Ver Salas: Staff (Admin + Psicólogos)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PSYCHOLOGIST')")
    public Flux<Room> getAll() {
        return roomService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PSYCHOLOGIST')")
    public Mono<Room> getById(@PathVariable Long id) {
        return roomService.findById(id);
    }

    // Gestión: Solo Admin
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Room> create(@RequestBody CreateRoomRequest request) {
        return roomService.createRoom(request.name());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Room> update(@PathVariable Long id, @RequestBody UpdateRoomRequest request) {
        return roomService.updateRoom(id, request.name());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Void> delete(@PathVariable Long id) {
        return roomService.deleteRoom(id);
    }

    // DTOs internos
    record CreateRoomRequest(String name) {}
    record UpdateRoomRequest(String name) {}
}
