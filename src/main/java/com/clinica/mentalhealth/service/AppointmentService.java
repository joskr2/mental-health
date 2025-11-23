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

    // Reglas de horario
    private static final LocalTime OPENING_TIME = LocalTime.of(8, 0);
    private static final LocalTime CLOSING_TIME = LocalTime.of(22, 0);

    /**
     * Obtener citas FILTRADAS por el rol del usuario actual.
     * - Admin: Ve todas.
     * - Psicólogo: Ve solo las suyas.
     * - Paciente: Ve solo las suyas.
     */
    public Flux<Appointment> getMyAppointments() {
        return ReactiveSecurityContextHolder.getContext()
                .mapNotNull(ctx -> Objects.requireNonNull(ctx.getAuthentication()).getPrincipal())
                .cast(UserPrincipal.class)
                .flatMapMany(user -> {
                    if ("ROLE_ADMIN".equals(user.role())) {
                        return appointmentRepository.findAll();
                    } else if ("ROLE_PSYCHOLOGIST".equals(user.role())) {
                        return appointmentRepository.findByPsychologistId(user.id());
                    } else if ("ROLE_PATIENT".equals(user.role())) {
                        return appointmentRepository.findByPatientId(user.id());
                    }
                    return Flux.empty();
                });
    }

    /**
     * Crear Cita.
     * - Admin/Psicólogo: Pueden agendar libremente (validando ids).
     * - Paciente: Solo puede agendar para SÍ MISMO (Su ID = patientId).
     */
    @Transactional
    public Mono<Appointment> createAppointment(Appointment appointment) {
        return ReactiveSecurityContextHolder.getContext()
                .mapNotNull(ctx -> Objects.requireNonNull(ctx.getAuthentication()).getPrincipal())
                .cast(UserPrincipal.class)
                .flatMap(user -> {

                    // Regla de Seguridad: Si es Paciente, no puede agendar a nombre de otro
                    if ("ROLE_PATIENT".equals(user.role())) {
                        if (!user.id().equals(appointment.patientId())) {
                            return Mono.error(new IllegalAccessException("Los pacientes solo pueden agendar sus propias citas."));
                        }
                    }

                    // Reglas de Negocio (Horarios)
                    try {
                        validateBusinessHours(appointment);
                    } catch (Exception e) {
                        return Mono.error(e);
                    }

                    // Cadena de Validaciones de Disponibilidad
                    return validatePsychologistAvailability(appointment)
                            .then(validatePatientAvailability(appointment))
                            .then(validateRoomAvailability(appointment))
                            .then(appointmentRepository.save(appointment));
                });
    }

    /**
     * Consultar disponibilidad de sala (Solo Psicólogos y Admin)
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'ROLE_PSYCHOLOGIST')")
    public Flux<Appointment> checkRoomAvailability(Long roomId, LocalDateTime date) {
        LocalDateTime startOfDay = date.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = date.toLocalDate().atTime(23, 59, 59);
        return appointmentRepository.findRoomConflicts(roomId, startOfDay, endOfDay);
    }

    // --- Validaciones Internas (Sin cambios) ---
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
                .flatMap(has -> has ? Mono.error(new IllegalStateException("Psicólogo ocupado.")) : Mono.empty());
    }

    private Mono<Void> validatePatientAvailability(Appointment appt) {
        return appointmentRepository.findPatientConflicts(appt.patientId(), appt.startTime(), appt.endTime())
                .hasElements()
                .flatMap(has -> has ? Mono.error(new IllegalStateException("Paciente ya tiene cita.")) : Mono.empty());
    }

    private Mono<Void> validateRoomAvailability(Appointment appt) {
        return appointmentRepository.findRoomConflicts(appt.roomId(), appt.startTime(), appt.endTime())
                .hasElements()
                .flatMap(has -> has ? Mono.error(new IllegalStateException("Sala ocupada.")) : Mono.empty());
    }
}