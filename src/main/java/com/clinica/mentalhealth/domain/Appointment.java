package com.clinica.mentalhealth.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Table("appointments")
public record Appointment(
        @Id Long id,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long patientId, // Relación lógica (Foreign Key)
        Long psychologistId, // Relación lógica
        Long roomId // Relación lógica
) {
    // Nota: La validación de fechas se hace en el servicio con Mono.error()
    // para evitar IllegalArgumentException durante deserialización JSON
    // que causaría errores 500 inesperados.
}
