package com.projecthelper.ai.tools;

import com.projecthelper.common.BusinessException;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TimeTools {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final Pattern ENGLISH_TIME = Pattern.compile("(?i)(today|tomorrow|day after tomorrow)?\\s*(?:at\\s*)?(\\d{1,2})(?::(\\d{1,2}))?\\s*(am|pm)?");

    @Tool(description = "Convert a natural-language deadline to a Unix seconds timestamp, such as tomorrow at 3 PM or 2026-08-01 18:00.")
    public long parseTime(@ToolParam(description = "Natural-language time") String text) {
        try {
            LocalDateTime exact = LocalDateTime.parse(text.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            return exact.atZone(ZONE).toEpochSecond();
        } catch (DateTimeParseException ignored) {}
        Matcher matcher = ENGLISH_TIME.matcher(text.trim());
        if (!matcher.matches()) throw BusinessException.badRequest("Unable to recognise the time. Provide a clear date and time");
        String day = matcher.group(1) == null ? "today" : matcher.group(1).toLowerCase();
        int offset = "day after tomorrow".equals(day) ? 2 : "tomorrow".equals(day) ? 1 : 0;
        int hour = Integer.parseInt(matcher.group(2));
        String meridiem = matcher.group(4);
        if (meridiem != null && meridiem.equalsIgnoreCase("pm") && hour < 12) hour += 12;
        if (meridiem != null && meridiem.equalsIgnoreCase("am") && hour == 12) hour = 0;
        int minute = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
        if (hour > 23 || minute > 59 || (meridiem != null && Integer.parseInt(matcher.group(2)) > 12)) throw BusinessException.badRequest("Invalid time format");
        return LocalDate.now(ZONE).plusDays(offset).atTime(hour, minute).atZone(ZONE).toEpochSecond();
    }

    @Tool(description = "Get the current China Standard Time and Unix seconds timestamp.")
    public String currentTime() {
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " (Unix seconds=" + now.toEpochSecond() + ")";
    }
}
