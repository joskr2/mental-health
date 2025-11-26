package com.clinica.mentalhealth.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos para actualizar un psicólogo existente")
public record UpdatePsychologistDto(
    @Schema(description = "Nombre completo del psicólogo", example = "Dr. Gregory House M.D.") String name,

    @Schema(description = "Especialidad médica", example = "Diagnóstico Diferencial") String specialty,

    @Schema(description = "Correo electrónico", example = "house@clinic.com") String email,

    @Schema(description = "Teléfono de contacto", example = "+51999888777") String phone,

    @Schema(description = "DNI del psicólogo", example = "87654321") String dni) {
}
