package com.projecthelper.progress;

import com.projecthelper.common.BusinessException;
import com.projecthelper.project.GraduationProject;
import com.projecthelper.project.ProjectRepository;
import com.projecthelper.security.CurrentUserService;
import com.projecthelper.user.User;
import com.projecthelper.user.UserRepository;
import com.projecthelper.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WeeklyReportService {
    private final WeeklyReportRepository reportRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public WeeklyReport create(ReportCommand command) {
        User student = requireStudent();
        GraduationProject project = projectRepository.findByStudentId(student.getId())
                .orElseThrow(() -> BusinessException.badRequest("Create a graduation project first"));
        validate(command, project);
        if (reportRepository.findByStudentIdAndWeekStart(student.getId(), command.weekStart()).isPresent()) {
            throw BusinessException.conflict("A report for this week already exists");
        }
        Instant now = Instant.now();
        return reportRepository.save(WeeklyReport.builder().studentId(student.getId()).mentorId(student.getMentorId())
                .projectId(project.getId()).weekStart(command.weekStart()).weekEnd(command.weekStart().plusDays(6))
                .completedWork(command.completedWork()).currentProblems(command.currentProblems())
                .nextWeekPlan(command.nextWeekPlan())
                .status(WeeklyReportStatus.DRAFT).createdAt(now).updatedAt(now).build());
    }

    public WeeklyReport update(String id, ReportCommand command) {
        User student = requireStudent();
        WeeklyReport report = ownedReport(id, student.getId());
        if (report.getStatus() != WeeklyReportStatus.DRAFT) throw BusinessException.conflict("Submitted reports cannot be edited");
        GraduationProject project = projectRepository.findByStudentId(student.getId())
                .orElseThrow(() -> BusinessException.badRequest("Create a graduation project first"));
        validate(command, project);
        if (!report.getWeekStart().equals(command.weekStart())
                && reportRepository.findByStudentIdAndWeekStart(student.getId(), command.weekStart()).isPresent()) {
            throw BusinessException.conflict("A report for the target week already exists");
        }
        report.setWeekStart(command.weekStart());
        report.setWeekEnd(command.weekStart().plusDays(6));
        report.setCompletedWork(command.completedWork());
        report.setCurrentProblems(command.currentProblems());
        report.setNextWeekPlan(command.nextWeekPlan());
        report.setUpdatedAt(Instant.now());
        return reportRepository.save(report);
    }

    public void delete(String id) {
        User student = requireStudent();
        WeeklyReport report = ownedReport(id, student.getId());
        if (report.getStatus() != WeeklyReportStatus.DRAFT) throw BusinessException.conflict("Only drafts can be deleted");
        reportRepository.delete(report);
    }

    public WeeklyReport submit(String id) {
        User student = requireStudent();
        WeeklyReport report = ownedReport(id, student.getId());
        if (report.getStatus() != WeeklyReportStatus.DRAFT) throw BusinessException.conflict("The report has already been submitted");
        report.setStatus(WeeklyReportStatus.SUBMITTED);
        report.setSubmittedAt(Instant.now());
        report.setUpdatedAt(Instant.now());
        return reportRepository.save(report);
    }

    public WeeklyReport review(String id, String content) {
        User mentor = currentUserService.require();
        if (mentor.getRole() != UserRole.MENTOR) throw BusinessException.forbidden("Only mentors can review weekly reports");
        WeeklyReport report = reportRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Weekly report not found"));
        if (!mentor.getId().equals(report.getMentorId())) throw BusinessException.forbidden("You cannot review a report belonging to another mentor's student");
        if (report.getStatus() == WeeklyReportStatus.DRAFT) throw BusinessException.conflict("Drafts cannot be reviewed");
        Instant now = Instant.now();
        Instant firstReviewedAt = report.getReview() == null ? now : report.getReview().getReviewedAt();
        report.setReview(WeeklyReport.Review.builder().mentorId(mentor.getId()).content(content.trim())
                .reviewedAt(firstReviewedAt).updatedAt(now).build());
        report.setStatus(WeeklyReportStatus.REVIEWED);
        report.setUpdatedAt(now);
        return reportRepository.save(report);
    }

    public Page<WeeklyReport> myReports(int page, int size) {
        User student = requireStudent();
        return reportRepository.findByStudentId(student.getId(), pageable(page, size));
    }

    public Page<WeeklyReport> mentorReports(WeeklyReportStatus status, int page, int size) {
        User mentor = currentUserService.require();
        if (mentor.getRole() != UserRole.MENTOR) throw BusinessException.forbidden("Only mentors can access this report");
        PageRequest request = pageable(page, size);
        if (status == WeeklyReportStatus.DRAFT) return Page.empty(request);
        return status == null ? reportRepository.findByMentorIdAndStatusIn(mentor.getId(),
                        List.of(WeeklyReportStatus.SUBMITTED, WeeklyReportStatus.REVIEWED), request)
                : reportRepository.findByMentorIdAndStatus(mentor.getId(), status, request);
    }

    public WeeklyReport get(String id) {
        User actor = currentUserService.require();
        WeeklyReport report = reportRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Weekly report not found"));
        boolean allowed = actor.getRole() == UserRole.ADMIN
                || actor.getId().equals(report.getStudentId())
                || actor.getId().equals(report.getMentorId());
        if (!allowed) throw BusinessException.forbidden("You are not allowed to view this weekly report");
        if (actor.getRole() == UserRole.MENTOR && report.getStatus() == WeeklyReportStatus.DRAFT) {
            throw BusinessException.forbidden("Draft reports are not available to mentors");
        }
        return report;
    }

    public java.util.List<WeeklyReport> reportsForAi(String studentId) {
        User actor = currentUserService.require();
        String target = actor.getRole() == UserRole.STUDENT ? actor.getId() : studentId;
        if (actor.getRole() == UserRole.MENTOR) {
            User student = userRepository.findById(target).orElseThrow(() -> BusinessException.notFound("Student not found"));
            if (!actor.getId().equals(student.getMentorId())) throw BusinessException.forbidden("The student is not assigned to this mentor");
        }
        if (actor.getRole() == UserRole.ADMIN) throw BusinessException.forbidden("Administrators cannot use AI to read student reports");
        return reportRepository.findByStudentId(target, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "weekStart")))
                .getContent().stream()
                .filter(report -> actor.getRole() != UserRole.MENTOR || report.getStatus() != WeeklyReportStatus.DRAFT)
                .toList();
    }

    private PageRequest pageable(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "weekStart"));
    }

    private WeeklyReport ownedReport(String id, String studentId) {
        WeeklyReport report = reportRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Weekly report not found"));
        if (!studentId.equals(report.getStudentId())) throw BusinessException.forbidden("You are not allowed to modify this weekly report");
        return report;
    }

    private User requireStudent() {
        User user = currentUserService.require();
        if (user.getRole() != UserRole.STUDENT) throw BusinessException.forbidden("Only students can submit weekly reports");
        return user;
    }

    private void validate(ReportCommand command, GraduationProject project) {
        if (project.getStartDate() == null || project.getPlannedEndDate() == null) {
            throw BusinessException.badRequest("Set the project start and planned end dates before creating a weekly report");
        }
        if (command.weekStart().getDayOfWeek() != DayOfWeek.MONDAY) {
            throw BusinessException.badRequest("weekStart must be a Monday");
        }
        LocalDate firstMonday = project.getStartDate().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate lastMonday = project.getPlannedEndDate().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if (command.weekStart().isBefore(firstMonday) || command.weekStart().isAfter(lastMonday)) {
            throw BusinessException.badRequest("The report week must fall within the project period");
        }
    }

    public record ReportCommand(LocalDate weekStart, String completedWork, String currentProblems,
                                String nextWeekPlan) {}
}
