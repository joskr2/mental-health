package com.clinica.mentalhealth.service;

import com.clinica.mentalhealth.domain.Appointment;
import com.clinica.mentalhealth.repository.AppointmentRepository;
import com.clinica.mentalhealth.security.UserPrincipal;
import com.clinica.mentalhealth.web.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

import static java.time.DayOfWeek.SUNDAY;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;

    private static final LocalTime OPENING_TIME = LocalTime.of(8, 0);
    private static final LocalTime CLOSING_TIME = LocalTime.of(22, 0);

    // Role constants to keep checks consistent
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_PSYCHOLOGIST = "ROLE_PSYCHOLOGIST";
    private static final String ROLE_PATIENT = "ROLE_PATIENT";

    // Helper to obtain current user reactively
    private Mono<UserPrincipal> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .mapNotNull(ctx -> Objects.requireNonNull(ctx.getAuthentication()).getPrincipal())
                .cast(UserPrincipal.class);
    }

    // Remove @Transactional: use reactive transactions if needed via R2DBC
    // TransactionalOperator
    public Mono<Appointment> createAppointment(Appointment appointment) {
        return currentUser()
                .flatMap(user -> {
                    if (ROLE_PATIENT.equals(user.role()) && !user.id().equals(appointment.patientId())) {
                        return Mono.error(
                                new IllegalAccessException("Los pacientes solo pueden agendar sus propias citas."));
                    }
                    return processAppointment(appointment);
                });
    }

    // Make parsing reactive and avoid try/catch on reactive flows
    public Mono<Appointment> createFromAi(Long patientId, Long psychologistId, String dateString) {
        return Mono.fromCallable(() -> LocalDateTime.parse(dateString))
                .map(start -> new Appointment(null, start, start.plusHours(1), patientId, psychologistId, 1L))
                .flatMap(this::processAppointment)
                .onErrorMap(e -> new IllegalArgumentException("Error al procesar datos de la IA: " + e.getMessage()));
    }

    public Flux<Appointment> getMyAppointments() {
        return currentUser()
                .flatMapMany(user -> {
                    if (ROLE_ADMIN.equals(user.role())) {
                        return appointmentRepository.findAll();
                    }
                    if (ROLE_PSYCHOLOGIST.equals(user.role())) {
                        return appointmentRepository.findByPsychologistId(user.id());
                    }
                    if (ROLE_PATIENT.equals(user.role())) {
                        return appointmentRepository.findByPatientId(user.id());
                    }
                    return Flux.empty();
                });
    }

    // hasAnyRole expects role names without the ROLE_ prefix by default
    @PreAuthorize("hasAnyRole('ADMIN','PSYCHOLOGIST')")
    public Flux<Appointment> checkRoomAvailability(Long roomId, LocalDateTime date) {
        LocalDateTime startOfDay = date.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = date.toLocalDate().atTime(23, 59, 59);
        return appointmentRepository.findRoomConflicts(roomId, startOfDay, endOfDay);
    }

    private Mono<Appointment> processAppointment(Appointment appointment) {
        return validateBusinessHours(appointment)
                .then(validatePsychologistAvailability(appointment))
                .then(validatePatientAvailability(appointment))
                .then(validateRoomAvailability(appointment))
                .then(appointmentRepository.save(java.util.Objects.requireNonNull(appointment)));
    }

    // Reactive validation instead of throwing exceptions synchronously
    private Mono<Void> validateBusinessHours(Appointment appointment) {
        var start = appointment.startTime();
        var end = appointment.endTime();

        if (start == null || end == null) {
            return Mono.error(new IllegalArgumentException("Horario de inicio/fin es requerido."));
        }
        if (!end.isAfter(start)) {
            return Mono.error(new IllegalArgumentException("La hora de fin debe ser después de la hora de inicio."));
        }
        if (start.getDayOfWeek() == SUNDAY) {
            return Mono.error(new IllegalArgumentException("Cerrado los domingos."));
        }
        var startTime = start.toLocalTime();
        var endTime = end.toLocalTime();
        if (startTime.isBefore(OPENING_TIME) || endTime.isAfter(CLOSING_TIME)) {
            return Mono.error(new IllegalArgumentException("Fuera de horario (08:00 - 22:00)."));
        }
        return Mono.empty();
    }

    private Mono<Void> failIfConflict(Flux<?> searchOperation, String errorMessage) {
        return searchOperation.hasElements()
                .flatMap(hasConflict -> Boolean.TRUE.equals(hasConflict)
                        ? Mono.error(new ConflictException(errorMessage))
                        : Mono.empty());
    }

    private Mono<Void> validatePsychologistAvailability(Appointment appointment) {
        return failIfConflict(
                appointmentRepository.findPsychologistConflicts(
                        appointment.psychologistId(), appointment.startTime(), appointment.endTime()),
                "Psicólogo ocupado.");
    }

    private Mono<Void> validatePatientAvailability(Appointment appointment) {
        return failIfConflict(
                appointmentRepository.findPatientConflicts(
                        appointment.patientId(), appointment.startTime(), appointment.endTime()),
                "Paciente ya tiene cita.");
    }

    private Mono<Void> validateRoomAvailability(Appointment appointment) {
        return failIfConflict(
                appointmentRepository.findRoomConflicts(
                        appointment.roomId(), appointment.startTime(), appointment.endTime()),
                "Sala ocupada.");
    }

    /**
     * Cancela una cita.
     * - Admin: puede cancelar cualquier cita
     * - Psicólogo: solo puede cancelar sus propias citas
     */
    public Mono<Void> cancelAppointment(Long appointmentId) {
        return currentUser()
                .flatMap(user -> appointmentRepository.findById(appointmentId)
                        .switchIfEmpty(
                                Mono.error(new IllegalArgumentException("Cita no encontrada con ID: " + appointmentId)))
                        .flatMap(appointment -> {
                            // Admin puede cancelar cualquier cita
                            if (ROLE_ADMIN.equals(user.role())) {
                                return appointmentRepository.deleteById(appointmentId);
                            }
                            // Psicólogo solo puede cancelar sus propias citas
                            if (ROLE_PSYCHOLOGIST.equals(user.role())) {
                                if (user.id().equals(appointment.psychologistId())) {
                                    return appointmentRepository.deleteById(appointmentId);
                                }
                                return Mono
                                        .error(new IllegalAccessException("Solo puedes cancelar tus propias citas."));
                            }
                            return Mono.error(new IllegalAccessException("No tienes permisos para cancelar citas."));
                        }));
    }
}
