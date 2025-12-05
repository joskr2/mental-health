package com.clinica.mentalhealth.ai.tools;

/**
 * Request para verificar disponibilidad de horarios.
 */
public record CheckAvailabilityRequest(
    Long psychologistId,
    String date  // Formato ISO-8601: "2025-12-10"
) {
}
