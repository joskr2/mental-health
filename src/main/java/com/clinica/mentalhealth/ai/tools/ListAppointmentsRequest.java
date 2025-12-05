package com.clinica.mentalhealth.ai.tools;

/**
 * Request para listar citas con filtros opcionales.
 * Si no se especifican filtros, retorna citas del usuario según su rol.
 */
public record ListAppointmentsRequest(
    Long patientId,      // Opcional: filtrar por paciente
    Long psychologistId, // Opcional: filtrar por psicólogo
    String startDate,    // Opcional: desde fecha (ISO-8601)
    String endDate       // Opcional: hasta fecha (ISO-8601)
) {
}
