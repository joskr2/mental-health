package com.clinica.mentalhealth.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request para cerrar sesión.
 * El refresh token es necesario para identificar qué sesión cerrar.
 */
@Schema(description = "Petición de logout")
public record LogoutRequest(
    @Schema(description = "Refresh token de la sesión a cerrar") String refreshToken,

    @Schema(description = "Si es true, cierra sesión en TODOS los dispositivos", defaultValue = "false") boolean logoutAll) {
}
