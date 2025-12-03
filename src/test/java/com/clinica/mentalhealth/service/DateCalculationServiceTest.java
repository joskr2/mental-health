package com.clinica.mentalhealth.service;

import com.clinica.mentalhealth.ai.tools.DateCalculationRequest;
import com.clinica.mentalhealth.ai.tools.DateCalculationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests para DateCalculationService.
 *
 * Este servicio es crítico porque evita que el LLM "alucine" fechas incorrectas.
 * Los tests verifican que las expresiones en español se interpretan correctamente.
 */
@DisplayName("DateCalculationService Tests")
class DateCalculationServiceTest {

    private DateCalculationService dateCalculationService;
    private static final ZoneId ZONE_LIMA = ZoneId.of("America/Lima");

    @BeforeEach
    void setUp() {
        dateCalculationService = new DateCalculationService();
    }

    @Nested
    @DisplayName("Fechas relativas simples")
    class SimpleDateTests {

        @Test
        @DisplayName("'hoy' debe retornar la fecha actual")
        void shouldParseHoy() {
            // Arrange
            DateCalculationRequest request = new DateCalculationRequest("hoy a las 10:00", 10, 0);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            assertNotNull(response);
            LocalDate today = LocalDate.now(ZONE_LIMA);
            LocalDateTime expected = today.atTime(10, 0);

            // Si ya pasó la hora, debería ser mañana
            LocalDateTime now = LocalDateTime.now(ZONE_LIMA);
            if (expected.isBefore(now)) {
                expected = expected.plusDays(1);
            }

            assertEquals(expected.toLocalDate(), LocalDateTime.parse(response.isoDateTime()).toLocalDate());
            assertEquals(DateCalculationResponse.Confidence.HIGH, response.confidence());
        }

        @Test
        @DisplayName("'mañana' debe retornar el día siguiente")
        void shouldParseMañana() {
            // Arrange
            DateCalculationRequest request = new DateCalculationRequest("mañana a las 15:00", 15, 0);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            assertNotNull(response);
            LocalDate tomorrow = LocalDate.now(ZONE_LIMA).plusDays(1);
            LocalDateTime parsed = LocalDateTime.parse(response.isoDateTime());

            assertEquals(tomorrow, parsed.toLocalDate());
            assertEquals(15, parsed.getHour());
            assertEquals(0, parsed.getMinute());
            assertEquals(DateCalculationResponse.Confidence.HIGH, response.confidence());
        }

        @Test
        @DisplayName("'manana' (sin tilde) debe funcionar igual")
        void shouldParseMananaSinTilde() {
            // Arrange
            DateCalculationRequest request = new DateCalculationRequest("manana a las 9:00", 9, 0);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            LocalDate tomorrow = LocalDate.now(ZONE_LIMA).plusDays(1);
            LocalDateTime parsed = LocalDateTime.parse(response.isoDateTime());

            assertEquals(tomorrow, parsed.toLocalDate());
        }

        @Test
        @DisplayName("'pasado mañana' debe retornar dentro de 2 días")
        void shouldParsePasadoMañana() {
            // Arrange
            DateCalculationRequest request = new DateCalculationRequest("pasado mañana a las 11:30", 11, 30);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            LocalDate dayAfterTomorrow = LocalDate.now(ZONE_LIMA).plusDays(2);
            LocalDateTime parsed = LocalDateTime.parse(response.isoDateTime());

            assertEquals(dayAfterTomorrow, parsed.toLocalDate());
            assertEquals(11, parsed.getHour());
            assertEquals(30, parsed.getMinute());
        }
    }

    @Nested
    @DisplayName("Días de la semana")
    class WeekdayTests {

        @Test
        @DisplayName("'lunes' debe retornar el próximo lunes")
        void shouldParseLunes() {
            // Arrange
            DateCalculationRequest request = new DateCalculationRequest("el lunes a las 10:00", 10, 0);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            LocalDateTime parsed = LocalDateTime.parse(response.isoDateTime());
            assertEquals(DayOfWeek.MONDAY, parsed.getDayOfWeek());
            assertTrue(parsed.isAfter(LocalDateTime.now(ZONE_LIMA).minusDays(1)));
        }

        @Test
        @DisplayName("'próximo martes' debe retornar el siguiente martes")
        void shouldParseProximoMartes() {
            // Arrange
            DateCalculationRequest request = new DateCalculationRequest("próximo martes a las 16:00", 16, 0);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            LocalDateTime parsed = LocalDateTime.parse(response.isoDateTime());
            assertEquals(DayOfWeek.TUESDAY, parsed.getDayOfWeek());

            // Debe ser al menos el próximo martes desde hoy
            LocalDate nextTuesday = LocalDate.now(ZONE_LIMA).with(TemporalAdjusters.next(DayOfWeek.TUESDAY));
            assertTrue(parsed.toLocalDate().isAfter(LocalDate.now(ZONE_LIMA).minusDays(1)));
        }

        @Test
        @DisplayName("'miércoles' con tilde debe funcionar")
        void shouldParseMiercolesConTilde() {
            // Arrange
            DateCalculationRequest request = new DateCalculationRequest("miércoles a las 14:00", 14, 0);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            LocalDateTime parsed = LocalDateTime.parse(response.isoDateTime());
            assertEquals(DayOfWeek.WEDNESDAY, parsed.getDayOfWeek());
        }

        @Test
        @DisplayName("'miercoles' sin tilde debe funcionar")
        void shouldParseMiercolesSinTilde() {
            // Arrange
            DateCalculationRequest request = new DateCalculationRequest("miercoles a las 14:00", 14, 0);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            LocalDateTime parsed = LocalDateTime.parse(response.isoDateTime());
            assertEquals(DayOfWeek.WEDNESDAY, parsed.getDayOfWeek());
        }

        @Test
        @DisplayName("'viernes' debe retornar el próximo viernes")
        void shouldParseViernes() {
            // Arrange
            DateCalculationRequest request = new DateCalculationRequest("viernes a las 18:00", 18, 0);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            LocalDateTime parsed = LocalDateTime.parse(response.isoDateTime());
            assertEquals(DayOfWeek.FRIDAY, parsed.getDayOfWeek());
        }

        @Test
        @DisplayName("'sábado' debe retornar el próximo sábado")
        void shouldParseSabado() {
            // Arrange
            DateCalculationRequest request = new DateCalculationRequest("sábado a las 10:00", 10, 0);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            LocalDateTime parsed = LocalDateTime.parse(response.isoDateTime());
            assertEquals(DayOfWeek.SATURDAY, parsed.getDayOfWeek());
        }
    }

    @Nested
    @DisplayName("Expresiones con días")
    class DaysFromNowTests {

        @Test
        @DisplayName("'en 3 días' debe sumar 3 días")
        void shouldParseEn3Dias() {
            // Arrange
            DateCalculationRequest request = new DateCalculationRequest("en 3 días a las 10:00", 10, 0);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            LocalDate expectedDate = LocalDate.now(ZONE_LIMA).plusDays(3);
            LocalDateTime parsed = LocalDateTime.parse(response.isoDateTime());

            assertEquals(expectedDate, parsed.toLocalDate());
            assertEquals(3, response.daysFromNow());
        }

        @Test
        @DisplayName("'en 1 día' debe sumar 1 día")
        void shouldParseEn1Dia() {
            // Arrange
            DateCalculationRequest request = new DateCalculationRequest("en 1 día a las 9:00", 9, 0);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            LocalDate expectedDate = LocalDate.now(ZONE_LIMA).plusDays(1);
            LocalDateTime parsed = LocalDateTime.parse(response.isoDateTime());

            assertEquals(expectedDate, parsed.toLocalDate());
        }

        @Test
        @DisplayName("'en 7 días' debe sumar 7 días")
        void shouldParseEn7Dias() {
            // Arrange
            DateCalculationRequest request = new DateCalculationRequest("en 7 dias", 10, 0);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            LocalDate expectedDate = LocalDate.now(ZONE_LIMA).plusDays(7);
            LocalDateTime parsed = LocalDateTime.parse(response.isoDateTime());

            assertEquals(expectedDate, parsed.toLocalDate());
        }
    }

    @Nested
    @DisplayName("Formatos de hora")
    class TimeParsingTests {

        @Test
        @DisplayName("Formato 24h: '15:30'")
        void shouldParse24hFormat() {
            // Arrange
            DateCalculationRequest request = new DateCalculationRequest("mañana a las 15:30", null, null);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            LocalDateTime parsed = LocalDateTime.parse(response.isoDateTime());
            assertEquals(15, parsed.getHour());
            assertEquals(30, parsed.getMinute());
            assertEquals(DateCalculationResponse.Confidence.HIGH, response.confidence());
        }

        @Test
        @DisplayName("Formato 12h: '3pm'")
        void shouldParse12hFormatPM() {
            // Arrange
            DateCalculationRequest request = new DateCalculationRequest("mañana a las 3pm", null, null);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            LocalDateTime parsed = LocalDateTime.parse(response.isoDateTime());
            assertEquals(15, parsed.getHour()); // 3pm = 15:00
        }

        @Test
        @DisplayName("Formato 12h: '10am'")
        void shouldParse12hFormatAM() {
            // Arrange
            DateCalculationRequest request = new DateCalculationRequest("mañana a las 10am", null, null);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            LocalDateTime parsed = LocalDateTime.parse(response.isoDateTime());
            assertEquals(10, parsed.getHour());
        }

        @Test
        @DisplayName("Formato informal: 'a las 4' (asume PM en contexto clínico)")
        void shouldParseInformalTime() {
            // Arrange
            DateCalculationRequest request = new DateCalculationRequest("mañana a las 4", null, null);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            LocalDateTime parsed = LocalDateTime.parse(response.isoDateTime());
            assertEquals(16, parsed.getHour()); // 4 -> 16:00 (asume PM)
        }

        @Test
        @DisplayName("Keyword 'temprano' = 9:00")
        void shouldParseTemprano() {
            // Arrange
            DateCalculationRequest request = new DateCalculationRequest("mañana temprano", null, null);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            LocalDateTime parsed = LocalDateTime.parse(response.isoDateTime());
            assertEquals(9, parsed.getHour());
        }

        @Test
        @DisplayName("Keyword 'mediodía' = 12:00")
        void shouldParseMediodia() {
            // Arrange
            DateCalculationRequest request = new DateCalculationRequest("mañana al mediodía", null, null);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            LocalDateTime parsed = LocalDateTime.parse(response.isoDateTime());
            assertEquals(12, parsed.getHour());
        }

        @Test
        @DisplayName("Keyword 'tarde' = 16:00")
        void shouldParseTarde() {
            // Arrange
            DateCalculationRequest request = new DateCalculationRequest("mañana en la tarde", null, null);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            LocalDateTime parsed = LocalDateTime.parse(response.isoDateTime());
            assertEquals(16, parsed.getHour());
        }
    }

    @Nested
    @DisplayName("Niveles de confianza")
    class ConfidenceTests {

        @Test
        @DisplayName("Fecha y hora explícitas = HIGH confidence")
        void shouldReturnHighConfidence() {
            // Arrange
            DateCalculationRequest request = new DateCalculationRequest("mañana a las 10:00", null, null);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            assertEquals(DateCalculationResponse.Confidence.HIGH, response.confidence());
            assertNull(response.warning());
        }

        @Test
        @DisplayName("Solo fecha = MEDIUM confidence")
        void shouldReturnMediumConfidenceWhenOnlyDate() {
            // Arrange - Sin hora explícita, usa preferredHour
            DateCalculationRequest request = new DateCalculationRequest("mañana", 10, 0);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            assertEquals(DateCalculationResponse.Confidence.MEDIUM, response.confidence());
            assertNotNull(response.warning());
        }

        @Test
        @DisplayName("Descripción no interpretable = LOW confidence")
        void shouldReturnLowConfidenceWhenNotParseable() {
            // Arrange - Texto que no contiene fecha ni hora reconocible
            DateCalculationRequest request = new DateCalculationRequest("cuando pueda", 10, 0);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            assertEquals(DateCalculationResponse.Confidence.LOW, response.confidence());
            assertNotNull(response.warning());
            assertTrue(response.warning().contains("RECOMENDACIÓN"));
        }
    }

    @Nested
    @DisplayName("Horario comercial")
    class BusinessHoursTests {

        @Test
        @DisplayName("10:00 en día laborable = horario comercial")
        void shouldDetectBusinessHours() {
            // Arrange - Próximo lunes a las 10:00
            DateCalculationRequest request = new DateCalculationRequest("próximo lunes a las 10:00", 10, 0);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            assertTrue(response.isBusinessHours());
        }

        @Test
        @DisplayName("21:00 = fuera de horario comercial")
        void shouldDetectOutsideBusinessHours() {
            // Arrange
            DateCalculationRequest request = new DateCalculationRequest("mañana a las 21:00", 21, 0);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            assertFalse(response.isBusinessHours());
        }

        @Test
        @DisplayName("Sábado = fuera de horario comercial (fin de semana)")
        void shouldDetectWeekendAsNonBusiness() {
            // Arrange
            DateCalculationRequest request = new DateCalculationRequest("sábado a las 10:00", 10, 0);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            LocalDateTime parsed = LocalDateTime.parse(response.isoDateTime());
            assertEquals(DayOfWeek.SATURDAY, parsed.getDayOfWeek());
            assertFalse(response.isBusinessHours());
        }
    }

    @Nested
    @DisplayName("Próxima semana")
    class NextWeekTests {

        @Test
        @DisplayName("'próxima semana' debe retornar el próximo lunes")
        void shouldParseProximaSemana() {
            // Arrange
            DateCalculationRequest request = new DateCalculationRequest("próxima semana a las 9:00", 9, 0);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            LocalDateTime parsed = LocalDateTime.parse(response.isoDateTime());
            assertEquals(DayOfWeek.MONDAY, parsed.getDayOfWeek());

            // Debe ser al menos el próximo lunes
            LocalDate nextMonday = LocalDate.now(ZONE_LIMA).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
            assertTrue(!parsed.toLocalDate().isBefore(nextMonday));
        }

        @Test
        @DisplayName("'la semana que viene' debe retornar el próximo lunes")
        void shouldParseSemanaQueViene() {
            // Arrange
            DateCalculationRequest request = new DateCalculationRequest("la semana que viene a las 10:00", 10, 0);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            LocalDateTime parsed = LocalDateTime.parse(response.isoDateTime());
            assertEquals(DayOfWeek.MONDAY, parsed.getDayOfWeek());
        }
    }

    @Nested
    @DisplayName("Human Readable Output")
    class HumanReadableTests {

        @Test
        @DisplayName("Debe generar descripción legible en español")
        void shouldGenerateHumanReadableDescription() {
            // Arrange
            DateCalculationRequest request = new DateCalculationRequest("mañana a las 15:30", 15, 30);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            assertNotNull(response.humanReadable());
            assertTrue(response.humanReadable().contains("15:30"));
            // Debe contener el día de la semana en español
            assertTrue(
                response.humanReadable().toLowerCase().contains("lunes") ||
                response.humanReadable().toLowerCase().contains("martes") ||
                response.humanReadable().toLowerCase().contains("miércoles") ||
                response.humanReadable().toLowerCase().contains("jueves") ||
                response.humanReadable().toLowerCase().contains("viernes") ||
                response.humanReadable().toLowerCase().contains("sábado") ||
                response.humanReadable().toLowerCase().contains("domingo")
            );
        }

        @Test
        @DisplayName("Debe incluir día de la semana en español")
        void shouldIncludeDayOfWeekInSpanish() {
            // Arrange
            DateCalculationRequest request = new DateCalculationRequest("próximo lunes a las 10:00", 10, 0);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            assertEquals("lunes", response.dayOfWeek().toLowerCase());
        }
    }

    @Nested
    @DisplayName("Preferred Hours Fallback")
    class PreferredHoursTests {

        @Test
        @DisplayName("Debe usar preferredHour cuando no se especifica hora")
        void shouldUsePreferredHour() {
            // Arrange
            DateCalculationRequest request = new DateCalculationRequest("mañana", 14, 30);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            LocalDateTime parsed = LocalDateTime.parse(response.isoDateTime());
            assertEquals(14, parsed.getHour());
            assertEquals(30, parsed.getMinute());
        }

        @Test
        @DisplayName("Debe usar 10:00 por defecto cuando no hay preferencia")
        void shouldUseDefaultHourWhenNoPreference() {
            // Arrange
            DateCalculationRequest request = new DateCalculationRequest("mañana", null, null);

            // Act
            DateCalculationResponse response = dateCalculationService.calculate(request);

            // Assert
            LocalDateTime parsed = LocalDateTime.parse(response.isoDateTime());
            assertEquals(10, parsed.getHour());
            assertEquals(0, parsed.getMinute());
        }
    }
}
