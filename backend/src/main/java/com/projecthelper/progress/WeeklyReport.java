package com.projecthelper.progress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@Document("weekly_reports")
@CompoundIndex(name = "student_week_unique", def = "{'studentId':1,'weekStart':1}", unique = true)
public class WeeklyReport {
    @Id private String id;
    @Indexed private String studentId;
    @Indexed private String mentorId;
    private String projectId;
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private String completedWork;
    private String currentProblems;
    private String nextWeekPlan;
    @Indexed private WeeklyReportStatus status;
    private Instant submittedAt;
    private Review review;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Review {
        private String mentorId;
        private String content;
        private Instant reviewedAt;
        private Instant updatedAt;
    }
}
