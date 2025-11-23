package com.clinica.mentalhealth.web;

import com.clinica.mentalhealth.domain.Appointment;
import com.clinica.mentalhealth.service.AppointmentService;
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
public class AppointmentController {

    private final AppointmentService appointmentService;

    // GET: api/appointments
    // Inteligente: Devuelve todas si eres Admin, solo las tuyas si eres Doctor/Paciente.
    @GetMapping
    public Flux<Appointment> getAll() {
        return appointmentService.getMyAppointments();
    }

    // POST: api/appointments
    // Inteligente: Pacientes solo pueden crear para sí mismos. Doctores/Admin para cualquiera.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Appointment> create(@RequestBody Appointment appointment) {
        return appointmentService.createAppointment(appointment);
    }

    // NUEVO ENDPOINT (Requerimiento de Psicólogo)
    // GET: api/appointments/rooms/1/check?date=2025-11-23T08:00:00
    // Permite ver si la Sala 1 está ocupada en esa fecha completa.
    @GetMapping("/rooms/{roomId}/check")
    @PreAuthorize("hasAnyRole('ADMIN', 'ROLE_PSYCHOLOGIST')")
    public Flux<Appointment> checkRoom(
            @PathVariable Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
        return appointmentService.checkRoomAvailability(roomId, date);
    }
}