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
 */
public record DateCalculationResponse(
        String isoDateTime,
        String humanReadable,
        String dayOfWeek,
        int daysFromNow,
        boolean isBusinessHours
) {
    
    public static DateCalculationResponse from(LocalDateTime dateTime, LocalDateTime now) {
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
                businessHours
        );
    }
}
