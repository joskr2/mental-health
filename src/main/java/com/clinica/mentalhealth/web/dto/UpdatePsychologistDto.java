package com.clinica.mentalhealth.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para actualizar un psicólogo existente")
public record UpdatePsychologistDto(
    @Schema(description = "Nombre completo del psicólogo", example = "Dr. Gregory House M.D.")
    @Size(min = 2, max = 255, message = "El nombre debe tener entre 2 y 255 caracteres")
    String name,

    @Schema(description = "Especialidad médica", example = "Diagnóstico Diferencial")
    @Size(max = 255, message = "La especialidad no puede exceder 255 caracteres")
    String specialty,

    @Schema(description = "Correo electrónico", example = "house@clinic.com")
    @Email(message = "El correo electrónico debe ser válido")
    @Size(max = 255, message = "El correo no puede exceder 255 caracteres")
    String email,

    @Schema(description = "Teléfono de contacto", example = "+51999888777")
    @Pattern(regexp = "^\\+?\\d{6,20}$", message = "El teléfono debe contener entre 6 y 20 dígitos")
    String phone,

    @Schema(description = "DNI del psicólogo", example = "87654321")
    @Pattern(regexp = "^\\d{8}$", message = "El DNI debe tener exactamente 8 dígitos")
    String dni
) {}
