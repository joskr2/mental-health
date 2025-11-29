package com.clinica.mentalhealth.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request para refrescar tokens")
public record RefreshTokenRequest(
    @Schema(description = "Refresh token JWT", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    @NotBlank(message = "El refresh token es requerido")
    String refreshToken
) {}

