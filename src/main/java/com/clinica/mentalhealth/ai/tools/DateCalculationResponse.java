package com.clinica.mentalhealth.ai.tools;

import java.time.LocalDateTime;

/**
 * Respuesta del cálculo de fecha con información contextual para el LLM.
 *
 * @param isoDateTime     Fecha/hora calculada en formato ISO-8601
 * @param humanReadable   Descripción legible en español
 * @param dayOfWeek       Día de la semana calculado
 * @param daysFromNow     Días desde hoy (para validación)
 * @param isBusinessHours Si está dentro del horario laboral (8:00-20:00)
 * @param confidence      Nivel de confianza del parsing (HIGH, MEDIUM, LOW)
 * @param warning         Mensaje de advertencia si confidence es bajo
 *                        (nullable)
 */
public record DateCalculationResponse(
    String isoDateTime,
    String humanReadable,
    String dayOfWeek,
    int daysFromNow,
    boolean isBusinessHours,
    Confidence confidence,
    String warning) {

  /**
   * Nivel de confianza en la interpretación de la fecha.
   * LOW indica que el LLM debería pedir aclaración al usuario.
   */
  public enum Confidence {
    /** Fecha y hora explícitas detectadas */
    HIGH,
    /** Solo fecha o solo hora detectada, se usaron defaults */
    MEDIUM,
    /** Fallback completo a valores por defecto, pedir aclaración */
    LOW
  }

  public static DateCalculationResponse from(LocalDateTime dateTime, LocalDateTime now,
      Confidence confidence, String warning) {
    var dayOfWeekEs = dateTime.getDayOfWeek()
        .getDisplayName(java.time.format.TextStyle.FULL, new java.util.Locale("es", "ES"));

    int hour = dateTime.getHour();
    boolean businessHours = hour >= 8 && hour < 20
        && dateTime.getDayOfWeek().getValue() <= 5; // Lunes a Viernes

    long daysFrom = java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(), dateTime.toLocalDate());

    String humanReadable = String.format("%s %d de %s a las %02d:%02d",
        dayOfWeekEs,
        dateTime.getDayOfMonth(),
        dateTime.getMonth().getDisplayName(java.time.format.TextStyle.FULL, new java.util.Locale("es", "ES")),
        dateTime.getHour(),
        dateTime.getMinute());

    return new DateCalculationResponse(
        dateTime.toString(),
        humanReadable,
        dayOfWeekEs,
        (int) daysFrom,
        businessHours,
        confidence,
        warning);
  }

  /**
   * Método de conveniencia para respuestas de alta confianza.
   */
  public static DateCalculationResponse from(LocalDateTime dateTime, LocalDateTime now) {
    return from(dateTime, now, Confidence.HIGH, null);
  }
}
