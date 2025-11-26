package com.clinica.mentalhealth.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response con tokens JWT")
public record LoginResponse(
    @Schema(description = "Access Token JWT (30 minutos)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String accessToken,
    @Schema(description = "Refresh Token JWT (14 días)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String refreshToken
) {}
