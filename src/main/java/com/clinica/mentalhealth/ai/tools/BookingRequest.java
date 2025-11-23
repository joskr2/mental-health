package com.clinica.mentalhealth.ai.tools;

// La IA calculará la fecha exacta (ej: "2025-11-24T15:00:00") basándose en "el próximo lunes"
public record BookingRequest(Long patientId, Long psychologistId, String startTime) {}