package com.clinica.mentalhealth.web;

import com.clinica.mentalhealth.domain.Appointment;
import com.clinica.mentalhealth.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Gestión de citas médicas")
@SecurityRequirement(name = "Bearer Authentication")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping
    @Operation(summary = "Obtener mis citas", description = "Devuelve todas las citas si eres Admin, solo las propias si eres Psicólogo o Paciente")
    @ApiResponse(responseCode = "200", description = "Lista de citas obtenida exitosamente")
    @ApiResponse(responseCode = "401", description = "No autenticado")
    @ApiResponse(responseCode = "403", description = "Sin permisos")
    public Flux<Appointment> getAll() {
        return appointmentService.getMyAppointments();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear cita", description = "Crea una nueva cita. Pacientes solo pueden crear para sí mismos. Validaciones: horario comercial, conflictos de sala/psicólogo/paciente.")
    @ApiResponse(responseCode = "201", description = "Cita creada exitosamente")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o fuera de horario")
    @ApiResponse(responseCode = "403", description = "Sin permisos para crear esta cita")
    @ApiResponse(responseCode = "409", description = "Conflicto: sala, psicólogo o paciente ocupado")
    public Mono<Appointment> create(@RequestBody Appointment appointment) {
        return appointmentService.createAppointment(appointment);
    }

    @GetMapping("/rooms/{roomId}/check")
    @PreAuthorize("hasAnyRole('ADMIN', 'PSYCHOLOGIST')")
    @Operation(summary = "Verificar disponibilidad de sala", description = "Permite ver si una sala está ocupada en una fecha específica. Solo para Admin y Psicólogos.")
    @ApiResponse(responseCode = "200", description = "Lista de conflictos obtenida")
    @ApiResponse(responseCode = "401", description = "No autenticado")
    @ApiResponse(responseCode = "403", description = "Sin permisos (solo Admin y Psicólogos)")
    public Flux<Appointment> checkRoom(
            @Parameter(description = "ID de la sala a verificar") @PathVariable Long roomId,
            @Parameter(description = "Fecha y hora para verificar disponibilidad (formato ISO)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
        return appointmentService.checkRoomAvailability(roomId, date);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'PSYCHOLOGIST')")
    @Operation(summary = "Cancelar cita", description = "Cancela una cita existente. Admin puede cancelar cualquier cita, Psicólogo solo sus propias citas.")
    @ApiResponse(responseCode = "204", description = "Cita cancelada exitosamente")
    @ApiResponse(responseCode = "401", description = "No autenticado")
    @ApiResponse(responseCode = "403", description = "Sin permisos para cancelar esta cita")
    @ApiResponse(responseCode = "404", description = "Cita no encontrada")
    public Mono<Void> cancel(
            @Parameter(description = "ID de la cita a cancelar") @PathVariable Long id) {
        return appointmentService.cancelAppointment(id);
    }
}
