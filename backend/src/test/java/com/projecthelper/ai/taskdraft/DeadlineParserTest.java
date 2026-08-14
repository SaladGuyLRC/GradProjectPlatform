package com.projecthelper.ai.taskdraft;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DeadlineParserTest {
    private final DeadlineParser parser = new DeadlineParser(
            Clock.fixed(Instant.parse("2026-08-14T10:00:00Z"), ZoneOffset.UTC));

    @Test
    void parsesRelativeTimeInLondon() {
        DeadlineParser.ParseResult result = parser.parse("tomorrow at 11 AM");

        assertEquals(DeadlineParser.ParseStatus.PARSED, result.status());
        assertEquals(Instant.parse("2026-08-15T10:00:00Z"), result.deadlineAt());
        assertEquals("15 Aug 2026, 11:00", parser.display(result.deadlineAt()));
    }

    @Test
    void parsesExplicitBritishDate() {
        DeadlineParser.ParseResult result = parser.parse("17/08/2026 14:30");

        assertEquals(DeadlineParser.ParseStatus.PARSED, result.status());
        assertEquals(Instant.parse("2026-08-17T13:30:00Z"), result.deadlineAt());
    }

    @Test
    void parsesMonthFirstYearAfterTimeWithCompactMeridiem() {
        DeadlineParser.ParseResult result = parser.parse("on 3 September 11am 2026");

        assertEquals(DeadlineParser.ParseStatus.PARSED, result.status());
        assertEquals(Instant.parse("2026-09-03T10:00:00Z"), result.deadlineAt());
    }

    @Test
    void rejectsIncompleteOrInvalidExpressions() {
        DeadlineParser.ParseResult incomplete = parser.parse("tomorrow");
        DeadlineParser.ParseResult invalid = parser.parse("tomorrow at 29 PM");

        assertEquals(DeadlineParser.ParseStatus.UNRECOGNIZED, incomplete.status());
        assertEquals(DeadlineParser.ParseStatus.UNRECOGNIZED, invalid.status());
        assertNull(invalid.deadlineAt());
    }
}
