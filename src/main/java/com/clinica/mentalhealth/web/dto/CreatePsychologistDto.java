package com.clinica.mentalhealth.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos para crear un nuevo psicólogo")
public record CreatePsychologistDto(
    @Schema(description = "Nombre completo del psicólogo", example = "Dr. Gregory House") String name,

    @Schema(description = "Especialidad médica", example = "Diagnóstico") String specialty,

    @Schema(description = "Correo electrónico", example = "house@clinic.com") String email,

    @Schema(description = "Teléfono de contacto", example = "+51999888777") String phone,

    @Schema(description = "DNI del psicólogo", example = "87654321") String dni,

    @Schema(description = "Nombre de usuario para login", example = "house") String username,

    @Schema(description = "Contraseña para login", example = "vicodin123") String password) {
}
