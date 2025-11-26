package com.clinica.mentalhealth.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request para autenticación de usuario")
public record LoginRequest(
    @Schema(description = "Nombre de usuario", example = "admin")
    String username,
    @Schema(description = "Contraseña del usuario", example = "password123")
    String password
) {}
