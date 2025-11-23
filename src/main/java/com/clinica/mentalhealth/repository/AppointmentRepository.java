package com.clinica.mentalhealth.repository;

import com.clinica.mentalhealth.domain.Appointment;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import java.time.LocalDateTime;

public interface AppointmentRepository extends ReactiveCrudRepository<Appointment, Long> {

    @Query("SELECT * FROM \"appointments\" WHERE psychologist_id = :psychologistId AND ((start_time < :end) AND (end_time > :start))")
    Flux<Appointment> findPsychologistConflicts(Long psychologistId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT * FROM \"appointments\" WHERE patient_id = :patientId AND ((start_time < :end) AND (end_time > :start))")
    Flux<Appointment> findPatientConflicts(Long patientId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT * FROM \"appointments\" WHERE room_id = :roomId AND ((start_time < :end) AND (end_time > :start))")
    Flux<Appointment> findRoomConflicts(Long roomId, LocalDateTime start, LocalDateTime end);

    // NUEVO: Filtros por Rol
    Flux<Appointment> findByPatientId(Long patientId);
    Flux<Appointment> findByPsychologistId(Long psychologistId);
}