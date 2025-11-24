package com.clinica.mentalhealth.service;

import com.clinica.mentalhealth.domain.Appointment;
import com.clinica.mentalhealth.repository.AppointmentRepository;
import com.clinica.mentalhealth.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;

    private static final LocalTime OPENING_TIME = LocalTime.of(8, 0);
    private static final LocalTime CLOSING_TIME = LocalTime.of(22, 0);

    // =================================================================
    // 1. MÉTODOS PÚBLICOS (Puntos de entrada)
    // =================================================================

    /**
     * Entrada para API REST (Controllers).
     * Se encarga de la seguridad (contexto) y delega la lógica.
     */
    @Transactional
    public Mono<Appointment> createAppointment(Appointment appointment) {
        return ReactiveSecurityContextHolder.getContext()
                .mapNotNull(ctx -> Objects.requireNonNull(ctx.getAuthentication()).getPrincipal())
                .cast(UserPrincipal.class)
                .flatMap(user -> {
                    // Validación de Seguridad: Propiedad de datos
                    if ("ROLE_PATIENT".equals(user.role()) && !user.id().equals(appointment.patientId())) {
                        return Mono.error(new IllegalAccessException("Los pacientes solo pueden agendar sus propias citas."));
                    }
                    // Delegamos al núcleo de negocio
                    return processAppointment(appointment);
                });
    }

    /**
     * NUEVO: Entrada para IA (AiToolsConfig).
     * Convierte datos crudos y delega la lógica.
     */
    @Transactional
    public Mono<Appointment> createFromAi(Long patientId, Long psychologistId, String dateString) {
        try {
            // 1. Conversión de datos (Adapter)
            LocalDateTime start = LocalDateTime.parse(dateString);
            LocalDateTime end = start.plusHours(1); // Duración estándar

            // 2. Construcción del objeto (roomId=1 por defecto o lógica de asignación)
            Appointment appt = new Appointment(null, start, end, patientId, psychologistId, 1L);

            // 3. Delegamos al mismo núcleo de negocio (Reutilización total)
            // Nota: Aquí asumimos que la IA (o el Prompt) ya validó quién es el usuario.
            return processAppointment(appt);

        } catch (Exception e) {
            return Mono.error(new IllegalArgumentException("Error al procesar datos de la IA: " + e.getMessage()));
        }
    }

    public Flux<Appointment> getMyAppointments() {
        return ReactiveSecurityContextHolder.getContext()
                .mapNotNull(ctx -> Objects.requireNonNull(ctx.getAuthentication()).getPrincipal())
                .cast(UserPrincipal.class)
                .flatMapMany(user -> {
                    if ("ROLE_ADMIN".equals(user.role())) return appointmentRepository.findAll();
                    if ("ROLE_PSYCHOLOGIST".equals(user.role())) return appointmentRepository.findByPsychologistId(user.id());
                    if ("ROLE_PATIENT".equals(user.role())) return appointmentRepository.findByPatientId(user.id());
                    return Flux.empty();
                });
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ROLE_PSYCHOLOGIST')")
    public Flux<Appointment> checkRoomAvailability(Long roomId, LocalDateTime date) {
        LocalDateTime startOfDay = date.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = date.toLocalDate().atTime(23, 59, 59);
        return appointmentRepository.findRoomConflicts(roomId, startOfDay, endOfDay);
    }

    // =================================================================
    // 2. NÚCLEO DE NEGOCIO (Privado/Reutilizable)
    // =================================================================

    /**
     * Contiene la lógica pura de validación y persistencia.
     * Es agnóstico de si viene por REST o por IA.
     */
    private Mono<Appointment> processAppointment(Appointment appointment) {
        // 1. Validaciones Síncronas (Horarios)
        try {
            validateBusinessHours(appointment);
        } catch (Exception e) {
            return Mono.error(e);
        }

        // 2. Validaciones Asíncronas (Conflictos) y Guardado
        return validatePsychologistAvailability(appointment)
                .then(validatePatientAvailability(appointment))
                .then(validateRoomAvailability(appointment))
                .then(Mono.just(appointment))
                .flatMap(appointmentRepository::save);
    }

    // =================================================================
    // 3. VALIDACIONES (Helpers)
    // =================================================================

    private void validateBusinessHours(Appointment appt) {
        var start = appt.startTime();
        var end = appt.endTime();

        if (start.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("Cerrado los domingos.");
        }
        if (start.toLocalTime().isBefore(OPENING_TIME) || end.toLocalTime().isAfter(CLOSING_TIME)) {
            throw new IllegalArgumentException("Fuera de horario (08:00 - 22:00).");
        }
    }

    private Mono<Void> validatePsychologistAvailability(Appointment appt) {
        return appointmentRepository.findPsychologistConflicts(appt.psychologistId(), appt.startTime(), appt.endTime())
                .hasElements()
                .flatMap(has -> has.booleanValue() ? Mono.error(new IllegalStateException("Psicólogo ocupado.")) : Mono.empty());
    }

    private Mono<Void> validatePatientAvailability(Appointment appt) {
        return appointmentRepository.findPatientConflicts(appt.patientId(), appt.startTime(), appt.endTime())
                .hasElements()
                .flatMap(has -> has.booleanValue() ? Mono.error(new IllegalStateException("Paciente ya tiene cita.")) : Mono.empty());
    }

    private Mono<Void> validateRoomAvailability(Appointment appt) {
        return appointmentRepository.findRoomConflicts(appt.roomId(), appt.startTime(), appt.endTime())
                .hasElements()
                .flatMap(has -> has.booleanValue() ? Mono.error(new IllegalStateException("Sala ocupada.")) : Mono.empty());
    }
}
