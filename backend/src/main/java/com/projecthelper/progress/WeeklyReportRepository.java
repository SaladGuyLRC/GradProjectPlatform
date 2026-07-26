package com.projecthelper.progress;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface WeeklyReportRepository extends MongoRepository<WeeklyReport, String> {
    Optional<WeeklyReport> findByStudentIdAndWeekStart(String studentId, LocalDate weekStart);
    Page<WeeklyReport> findByStudentId(String studentId, Pageable pageable);
    Page<WeeklyReport> findByMentorId(String mentorId, Pageable pageable);
    Page<WeeklyReport> findByMentorIdAndStatus(String mentorId, WeeklyReportStatus status, Pageable pageable);
    Optional<WeeklyReport> findFirstByStudentIdOrderByWeekStartDesc(String studentId);
}
