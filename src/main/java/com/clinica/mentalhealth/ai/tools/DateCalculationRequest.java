package com.clinica.mentalhealth.ai.tools;

/**
 * Request para calcular fechas relativas de forma determinística.
 * El LLM describe la fecha en lenguaje natural y Java la calcula exactamente.
 * 
 * @param relativeDescription Descripción relativa como "próximo lunes a las 4pm",
 *                            "mañana a las 10:00", "en 3 días a las 15:30"
 * @param preferredHour       Hora preferida (0-23) si no se especifica en la descripción
 * @param preferredMinute     Minuto preferido (0-59) si no se especifica
 */
public record DateCalculationRequest(
        String relativeDescription,
        Integer preferredHour,
        Integer preferredMinute
) {}
