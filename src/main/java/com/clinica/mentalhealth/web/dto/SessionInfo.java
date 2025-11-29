package com.clinica.mentalhealth.web.dto;

import java.time.Instant;

/**
 * DTO para mostrar información de una sesión activa al usuario.
 * No expone información sensible como el token completo.
 */
public record SessionInfo(
    String sessionId, // ID de sesión (para revocar específicamente)
    String deviceInfo, // Información del dispositivo/navegador
    String ipAddress, // IP desde donde se creó la sesión
    Instant createdAt // Fecha de inicio de sesión
) {
}
