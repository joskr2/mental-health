package com.clinica.mentalhealth.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request para cerrar sesión.
 * El refresh token es necesario para identificar qué sesión cerrar.
 */
@Schema(description = "Petición de logout")
public record LogoutRequest(
    @Schema(description = "Refresh token de la sesión a cerrar")
    @NotBlank(message = "El refresh token es requerido")
    String refreshToken,

    @Schema(description = "Si es true, cierra sesión en TODOS los dispositivos", defaultValue = "false")
    boolean logoutAll
) {}
