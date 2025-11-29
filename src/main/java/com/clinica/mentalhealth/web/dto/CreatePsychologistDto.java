package com.clinica.mentalhealth.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para crear un nuevo psicólogo")
public record CreatePsychologistDto(
        @Schema(description = "Nombre completo del psicólogo", example = "Dr. Gregory House") @NotBlank(message = "El nombre es requerido") @Size(min = 2, max = 255, message = "El nombre debe tener entre 2 y 255 caracteres") String name,

        @Schema(description = "Especialidad médica", example = "Diagnóstico") @Size(max = 255, message = "La especialidad no puede exceder 255 caracteres") String specialty,

        @Schema(description = "Correo electrónico", example = "house@clinic.com") @Email(message = "El correo electrónico debe ser válido") @Size(max = 255, message = "El correo no puede exceder 255 caracteres") String email,

        @Schema(description = "Teléfono de contacto", example = "+51999888777") @Pattern(regexp = "^\\+?\\d{6,20}$", message = "El teléfono debe contener entre 6 y 20 dígitos") String phone,

        @Schema(description = "DNI del psicólogo", example = "87654321") @NotBlank(message = "El DNI es requerido") @Pattern(regexp = "^\\d{8}$", message = "El DNI debe tener exactamente 8 dígitos") String dni,

        @Schema(description = "Nombre de usuario para login", example = "house") @NotBlank(message = "El nombre de usuario es requerido") @Size(min = 3, max = 50, message = "El usuario debe tener entre 3 y 50 caracteres") String username,

        @Schema(description = "Contraseña para login", example = "vicodin123") @NotBlank(message = "La contraseña es requerida") @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres") String password) {
}
