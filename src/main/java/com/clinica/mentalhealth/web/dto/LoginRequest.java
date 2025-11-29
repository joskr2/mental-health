package com.clinica.mentalhealth.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request para autenticación de usuario")
public record LoginRequest(
        @Schema(description = "Nombre de usuario", example = "admin") @NotBlank(message = "El nombre de usuario es requerido") @Size(min = 3, max = 50, message = "El nombre de usuario debe tener entre 3 y 50 caracteres") String username,

        @Schema(description = "Contraseña del usuario", example = "password123") @NotBlank(message = "La contraseña es requerida") @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres") String password) {
}
