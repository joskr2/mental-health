package com.clinica.mentalhealth.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Table("appointments")
public record Appointment(
        @Id Long id,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long patientId,      // Relación lógica (Foreign Key)
        Long psychologistId, // Relación lógica
        Long roomId          // Relación lógica
) {
    // Constructor compacto para validaciones
    public Appointment {
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("La hora de fin no puede ser anterior a la de inicio");
        }
    }
}