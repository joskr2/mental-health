package com.clinica.mentalhealth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de configuración para seguridad de tokens.
 */
@ConfigurationProperties(prefix = "security.refresh-token")
public record SecurityProperties(
    /**
     * Número máximo de sesiones activas por usuario.
     * 0 = sin límite.
     */
    int maxSessions) {
  public SecurityProperties {
    // Valor por defecto si no se especifica
    if (maxSessions < 0) {
      maxSessions = 5;
    }
  }

  // Constructor con valor por defecto
  public SecurityProperties() {
    this(5);
  }
}
