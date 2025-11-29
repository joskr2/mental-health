package com.clinica.mentalhealth.service;

import com.clinica.mentalhealth.ai.tools.DateCalculationRequest;
import com.clinica.mentalhealth.ai.tools.DateCalculationResponse;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servicio para cálculo determinístico de fechas relativas.
 * Elimina la alucinación del LLM al calcular fechas con Java.
 */
@Service
public class DateCalculationService {

  private static final Map<String, DayOfWeek> DIAS_SEMANA = Map.of(
      "lunes", DayOfWeek.MONDAY,
      "martes", DayOfWeek.TUESDAY,
      "miércoles", DayOfWeek.WEDNESDAY,
      "miercoles", DayOfWeek.WEDNESDAY,
      "jueves", DayOfWeek.THURSDAY,
      "viernes", DayOfWeek.FRIDAY,
      "sábado", DayOfWeek.SATURDAY,
      "sabado", DayOfWeek.SATURDAY,
      "domingo", DayOfWeek.SUNDAY);

  private static final Pattern TIME_24H = Pattern.compile("(\\d{1,2}):(\\d{2})");
  private static final Pattern TIME_12H = Pattern.compile("(\\d{1,2})\\s*(am|pm|a\\.m\\.|p\\.m\\.)",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern TIME_INFORMAL = Pattern.compile("a las (\\d{1,2})", Pattern.CASE_INSENSITIVE);
  private static final Pattern EN_DIAS = Pattern.compile("en (\\d+) d[íi]as?");

  public DateCalculationResponse calculate(DateCalculationRequest request) {
    LocalDateTime now = LocalDateTime.now();
    String desc = request.relativeDescription().toLowerCase(Locale.ROOT);

    LocalDate targetDate = parseRelativeDate(desc, now.toLocalDate());
    LocalTime targetTime = parseTime(desc, request.preferredHour(), request.preferredMinute());

    LocalDateTime result = LocalDateTime.of(targetDate, targetTime);

    if (result.isBefore(now)) {
      result = adjustToFuture(result, now);
    }

    return DateCalculationResponse.from(result, now);
  }

  private LocalDate parseRelativeDate(String desc, LocalDate today) {
    return parseSimpleRelativeDate(desc, today)
        .or(() -> parseDaysFromNow(desc, today))
        .or(() -> parseWeekDay(desc, today))
        .or(() -> parseNextWeek(desc, today))
        .orElse(today.plusDays(1));
  }

  private Optional<LocalDate> parseSimpleRelativeDate(String desc, LocalDate today) {
    if (desc.contains("hoy")) {
      return Optional.of(today);
    }
    if (desc.contains("pasado mañana") || desc.contains("pasado manana")) {
      return Optional.of(today.plusDays(2));
    }
    if (desc.contains("mañana") || desc.contains("manana")) {
      return Optional.of(today.plusDays(1));
    }
    return Optional.empty();
  }

  private Optional<LocalDate> parseDaysFromNow(String desc, LocalDate today) {
    Matcher matcher = EN_DIAS.matcher(desc);
    if (matcher.find()) {
      return Optional.of(today.plusDays(Integer.parseInt(matcher.group(1))));
    }
    return Optional.empty();
  }

  private Optional<LocalDate> parseWeekDay(String desc, LocalDate today) {
    for (var entry : DIAS_SEMANA.entrySet()) {
      if (desc.contains(entry.getKey())) {
        return Optional.of(calculateWeekDay(desc, today, entry.getValue()));
      }
    }
    return Optional.empty();
  }

  private LocalDate calculateWeekDay(String desc, LocalDate today, DayOfWeek targetDay) {
    boolean nextWeek = desc.contains("próximo") || desc.contains("proximo")
        || desc.contains("siguiente") || desc.contains("que viene");

    if (nextWeek) {
      return today.with(TemporalAdjusters.next(targetDay));
    }

    LocalDate result = today.with(TemporalAdjusters.nextOrSame(targetDay));
    if (result.equals(today) && desc.contains("este")) {
      return today;
    }
    return result;
  }

  private Optional<LocalDate> parseNextWeek(String desc, LocalDate today) {
    if (desc.contains("próxima semana") || desc.contains("proxima semana")
        || desc.contains("la semana que viene")) {
      return Optional.of(today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)));
    }
    return Optional.empty();
  }

  private LocalTime parseTime(String desc, Integer preferredHour, Integer preferredMinute) {
    return parseTime24h(desc)
        .or(() -> parseTime12h(desc))
        .or(() -> parseTimeInformal(desc))
        .or(() -> parseTimeKeywords(desc))
        .orElseGet(() -> buildDefaultTime(preferredHour, preferredMinute));
  }

  private Optional<LocalTime> parseTime24h(String desc) {
    Matcher m = TIME_24H.matcher(desc);
    if (m.find()) {
      int hour = Math.min(Integer.parseInt(m.group(1)), 23);
      int minute = Math.min(Integer.parseInt(m.group(2)), 59);
      return Optional.of(LocalTime.of(hour, minute));
    }
    return Optional.empty();
  }

  private Optional<LocalTime> parseTime12h(String desc) {
    Matcher m = TIME_12H.matcher(desc);
    if (m.find()) {
      int hour = Integer.parseInt(m.group(1));
      boolean isPm = m.group(2).toLowerCase().startsWith("p");
      hour = convertTo24h(hour, isPm);
      return Optional.of(LocalTime.of(hour, 0));
    }
    return Optional.empty();
  }

  private int convertTo24h(int hour, boolean isPm) {
    if (isPm && hour != 12)
      return hour + 12;
    if (!isPm && hour == 12)
      return 0;
    return Math.min(hour, 23);
  }

  private Optional<LocalTime> parseTimeInformal(String desc) {
    Matcher m = TIME_INFORMAL.matcher(desc);
    if (m.find()) {
      int hour = Integer.parseInt(m.group(1));
      if (hour < 8)
        hour += 12; // Asumir PM en contexto clínico
      return Optional.of(LocalTime.of(Math.min(hour, 23), 0));
    }
    return Optional.empty();
  }

  private Optional<LocalTime> parseTimeKeywords(String desc) {
    if (desc.contains("temprano"))
      return Optional.of(LocalTime.of(9, 0));
    if (desc.contains("mediodía") || desc.contains("mediodia"))
      return Optional.of(LocalTime.of(12, 0));
    if (desc.contains("tarde"))
      return Optional.of(LocalTime.of(16, 0));
    if (desc.contains("noche"))
      return Optional.of(LocalTime.of(19, 0));
    return Optional.empty();
  }

  private LocalTime buildDefaultTime(Integer preferredHour, Integer preferredMinute) {
    int hour = (preferredHour != null) ? Math.min(preferredHour, 23) : 10;
    int minute = (preferredMinute != null) ? Math.min(preferredMinute, 59) : 0;
    return LocalTime.of(hour, minute);
  }

  private LocalDateTime adjustToFuture(LocalDateTime result, LocalDateTime now) {
    if (result.toLocalDate().equals(now.toLocalDate())) {
      return result.plusDays(1);
    }
    return result.plusWeeks(1);
  }
}
