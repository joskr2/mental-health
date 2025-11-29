package com.clinica.mentalhealth.web;

import com.clinica.mentalhealth.domain.Room;
import com.clinica.mentalhealth.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Tag(name = "Salas", description = "Gestión de salas/consultorios de la clínica")
@SecurityRequirement(name = "Bearer Authentication")
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PSYCHOLOGIST')")
    @Operation(summary = "Listar salas", description = "Obtiene todas las salas disponibles")
    public Flux<Room> getAll() {
        return roomService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PSYCHOLOGIST')")
    @Operation(summary = "Obtener sala por ID", description = "Busca una sala específica por su identificador")
    public Mono<Room> getById(@PathVariable @NonNull Long id) {
        return roomService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear sala", description = "Crea una nueva sala (solo administradores)")
    public Mono<Room> create(@RequestBody CreateRoomRequest request) {
        return roomService.createRoom(request.name());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar sala", description = "Modifica el nombre de una sala existente")
    public Mono<Room> update(@PathVariable @NonNull Long id, @RequestBody UpdateRoomRequest request) {
        return roomService.updateRoom(id, request.name());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar sala", description = "Elimina una sala del sistema")
    public Mono<Void> delete(@PathVariable @NonNull Long id) {
        return roomService.deleteRoom(id);
    }

    // DTOs internos
    record CreateRoomRequest(String name) {
    }

    record UpdateRoomRequest(String name) {
    }
}
