package com.projecthelper.ai.taskdraft;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DeadlineParser {
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/London");
    private static final List<String> SUPPORTED_EXAMPLES = List.of(
            "3 September 2026 at 19:00",
            "3 Sep 2026 at 7pm",
            "3 Sep 7pm 2026",
            "tomorrow at 11am",
            "next Monday at 09:00",
            "2026-09-03 19:00");

    private static final Pattern RELATIVE = Pattern.compile(
            "(?i)^(today|tomorrow|day after tomorrow)(?:\\s+at)?\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?$");
    private static final Pattern NEXT_WEEKDAY = Pattern.compile(
            "(?i)^next\\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday)(?:\\s+at)?\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?$");
    private static final List<DateTimeFormatter> EXACT_FORMATS = List.of(
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(java.time.format.ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm").withResolverStyle(java.time.format.ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d/M/uuuu H:mm").withResolverStyle(java.time.format.ResolverStyle.STRICT),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d MMM uuuu H:mm")
                    .toFormatter(Locale.UK).withResolverStyle(java.time.format.ResolverStyle.STRICT),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d MMMM uuuu H:mm")
                    .toFormatter(Locale.UK).withResolverStyle(java.time.format.ResolverStyle.STRICT),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d MMM uuuu h")
                    .optionalStart().appendPattern(":mm").optionalEnd().appendLiteral(' ')
                    .appendText(ChronoField.AMPM_OF_DAY).toFormatter(Locale.UK),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d MMMM uuuu h")
                    .optionalStart().appendPattern(":mm").optionalEnd().appendLiteral(' ')
                    .appendText(ChronoField.AMPM_OF_DAY).toFormatter(Locale.UK),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d MMM h")
                    .optionalStart().appendPattern(":mm").optionalEnd().appendLiteral(' ')
                    .appendText(ChronoField.AMPM_OF_DAY).appendLiteral(' ').appendPattern("uuuu")
                    .toFormatter(Locale.UK),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d MMMM h")
                    .optionalStart().appendPattern(":mm").optionalEnd().appendLiteral(' ')
                    .appendText(ChronoField.AMPM_OF_DAY).appendLiteral(' ').appendPattern("uuuu")
                    .toFormatter(Locale.UK)
    );

    private final Clock clock;

    public DeadlineParser() {
        this(Clock.systemUTC());
    }

    DeadlineParser(Clock clock) {
        this.clock = clock;
    }

    public ParseResult parse(String text) {
        // 先尝试确定格式，再处理相对日期；所有结果最终统一转换为英国时区的 Instant。
        if (text == null || text.isBlank()) return ParseResult.unrecognized();
        String value = text.trim()
                .replaceAll("(?i)\\s+at\\s+", " ")
                .replaceAll("(?i)(\\d)(am|pm)\\b", "$1 $2")
                .replaceFirst("(?i)^on\\s+", "")
                .replaceAll("\\s+", " ");

        for (DateTimeFormatter formatter : EXACT_FORMATS) {
            try {
                return atBusinessZone(LocalDateTime.parse(value, formatter));
            } catch (DateTimeParseException ignored) {
                // Try the next supported format.
            }
        }

        Matcher relative = RELATIVE.matcher(text.trim());
        if (relative.matches()) {
            int days = switch (relative.group(1).toLowerCase(Locale.UK)) {
                case "tomorrow" -> 1;
                case "day after tomorrow" -> 2;
                default -> 0;
            };
            return relativeResult(LocalDate.now(clock.withZone(BUSINESS_ZONE)).plusDays(days), relative, 2);
        }

        Matcher weekday = NEXT_WEEKDAY.matcher(text.trim());
        if (weekday.matches()) {
            DayOfWeek day = DayOfWeek.valueOf(weekday.group(1).toUpperCase(Locale.UK));
            LocalDate date = LocalDate.now(clock.withZone(BUSINESS_ZONE)).with(TemporalAdjusters.next(day));
            return relativeResult(date, weekday, 2);
        }
        return ParseResult.unrecognized();
    }

    public Instant now() {
        return clock.instant();
    }

    public String display(Instant instant) {
        return DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm", Locale.UK)
                .format(instant.atZone(BUSINESS_ZONE));
    }

    public String supportedExamplesMessage() {
        return "Supported formats include:\n- " + String.join("\n- ", SUPPORTED_EXAMPLES)
                + "\nAll times are interpreted in UK time (Europe/London).";
    }

    private ParseResult relativeResult(LocalDate date, Matcher matcher, int hourGroup) {
        try {
            int originalHour = Integer.parseInt(matcher.group(hourGroup));
            int minute = matcher.group(hourGroup + 1) == null ? 0 : Integer.parseInt(matcher.group(hourGroup + 1));
            String meridiem = matcher.group(hourGroup + 2);
            if (minute > 59 || originalHour > (meridiem == null ? 23 : 12) || originalHour == 0 && meridiem != null) {
                return ParseResult.unrecognized();
            }
            int hour = originalHour;
            if ("pm".equalsIgnoreCase(meridiem) && hour < 12) hour += 12;
            if ("am".equalsIgnoreCase(meridiem) && hour == 12) hour = 0;
            return atBusinessZone(LocalDateTime.of(date, LocalTime.of(hour, minute)));
        } catch (DateTimeException | NumberFormatException exception) {
            return ParseResult.unrecognized();
        }
    }

    private ParseResult atBusinessZone(LocalDateTime localDateTime) {
        // 夏令时切换可能产生不存在或重复的本地时间，遇到这两类情况不擅自猜测。
        List<ZoneOffset> offsets = BUSINESS_ZONE.getRules().getValidOffsets(localDateTime);
        if (offsets.size() != 1) return ParseResult.ambiguous();
        ZonedDateTime zoned = ZonedDateTime.ofStrict(localDateTime, offsets.getFirst(), BUSINESS_ZONE);
        return new ParseResult(ParseStatus.PARSED, zoned.toInstant());
    }

    public enum ParseStatus { PARSED, AMBIGUOUS, UNRECOGNIZED }

    public record ParseResult(ParseStatus status, Instant deadlineAt) {
        static ParseResult ambiguous() { return new ParseResult(ParseStatus.AMBIGUOUS, null); }
        static ParseResult unrecognized() { return new ParseResult(ParseStatus.UNRECOGNIZED, null); }
    }
}
