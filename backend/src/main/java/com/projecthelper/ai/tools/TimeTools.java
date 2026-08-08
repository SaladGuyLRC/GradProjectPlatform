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
    private static final Pattern CHINESE_TIME = Pattern.compile("(今天|明天|后天)?(?:上午|下午|晚上)?(\\d{1,2})点(?:(\\d{1,2})分)?");

    @Tool(description = "Convert a natural-language deadline to a Unix seconds timestamp, such as tomorrow at 3 PM or 2026-08-01 18:00.")
    public long parseTime(@ToolParam(description = "Natural-language time") String text) {
        try {
            LocalDateTime exact = LocalDateTime.parse(text.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            return exact.atZone(ZONE).toEpochSecond();
        } catch (DateTimeParseException ignored) {}
        Matcher matcher = CHINESE_TIME.matcher(text.trim());
        if (!matcher.find()) throw BusinessException.badRequest("无法识别时间，请提供明确日期和时间");
        int offset = "后天".equals(matcher.group(1)) ? 2 : "明天".equals(matcher.group(1)) ? 1 : 0;
        int hour = Integer.parseInt(matcher.group(2));
        if ((text.contains("下午") || text.contains("晚上")) && hour < 12) hour += 12;
        int minute = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
        if (hour > 23 || minute > 59) throw BusinessException.badRequest("时间格式不合法");
        return LocalDate.now(ZONE).plusDays(offset).atTime(hour, minute).atZone(ZONE).toEpochSecond();
    }

    @Tool(description = "Get the current China Standard Time and Unix seconds timestamp.")
    public String currentTime() {
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "，Unix秒=" + now.toEpochSecond();
    }
}
