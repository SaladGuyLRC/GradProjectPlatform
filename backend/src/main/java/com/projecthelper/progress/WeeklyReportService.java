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
                .orElseThrow(() -> BusinessException.badRequest("请先创建毕设项目"));
        validate(command);
        if (reportRepository.findByStudentIdAndWeekStart(student.getId(), command.weekStart()).isPresent()) {
            throw BusinessException.conflict("本周进展已存在");
        }
        Instant now = Instant.now();
        return reportRepository.save(WeeklyReport.builder().studentId(student.getId()).mentorId(student.getMentorId())
                .projectId(project.getId()).weekStart(command.weekStart()).weekEnd(command.weekStart().plusDays(6))
                .completedWork(command.completedWork()).currentProblems(command.currentProblems())
                .nextWeekPlan(command.nextWeekPlan()).progressPercentage(command.progressPercentage())
                .status(WeeklyReportStatus.DRAFT).createdAt(now).updatedAt(now).build());
    }

    public WeeklyReport update(String id, ReportCommand command) {
        User student = requireStudent();
        WeeklyReport report = ownedReport(id, student.getId());
        if (report.getStatus() != WeeklyReportStatus.DRAFT) throw BusinessException.conflict("进展提交后不能修改");
        validate(command);
        if (!report.getWeekStart().equals(command.weekStart())
                && reportRepository.findByStudentIdAndWeekStart(student.getId(), command.weekStart()).isPresent()) {
            throw BusinessException.conflict("目标周进展已存在");
        }
        report.setWeekStart(command.weekStart());
        report.setWeekEnd(command.weekStart().plusDays(6));
        report.setCompletedWork(command.completedWork());
        report.setCurrentProblems(command.currentProblems());
        report.setNextWeekPlan(command.nextWeekPlan());
        report.setProgressPercentage(command.progressPercentage());
        report.setUpdatedAt(Instant.now());
        return reportRepository.save(report);
    }

    public void delete(String id) {
        User student = requireStudent();
        WeeklyReport report = ownedReport(id, student.getId());
        if (report.getStatus() != WeeklyReportStatus.DRAFT) throw BusinessException.conflict("仅草稿可以删除");
        reportRepository.delete(report);
    }

    public WeeklyReport submit(String id) {
        User student = requireStudent();
        WeeklyReport report = ownedReport(id, student.getId());
        if (report.getStatus() != WeeklyReportStatus.DRAFT) throw BusinessException.conflict("进展已经提交");
        report.setStatus(WeeklyReportStatus.SUBMITTED);
        report.setSubmittedAt(Instant.now());
        report.setUpdatedAt(Instant.now());
        return reportRepository.save(report);
    }

    public WeeklyReport review(String id, String content) {
        User mentor = currentUserService.require();
        if (mentor.getRole() != UserRole.MENTOR) throw BusinessException.forbidden("仅导师可以评阅周进展");
        WeeklyReport report = reportRepository.findById(id).orElseThrow(() -> BusinessException.notFound("周进展不存在"));
        if (!mentor.getId().equals(report.getMentorId())) throw BusinessException.forbidden("不能评阅其他导师学生的进展");
        if (report.getStatus() == WeeklyReportStatus.DRAFT) throw BusinessException.conflict("草稿不能评阅");
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
        if (mentor.getRole() != UserRole.MENTOR) throw BusinessException.forbidden("仅导师可访问");
        return status == null ? reportRepository.findByMentorId(mentor.getId(), pageable(page, size))
                : reportRepository.findByMentorIdAndStatus(mentor.getId(), status, pageable(page, size));
    }

    public WeeklyReport get(String id) {
        User actor = currentUserService.require();
        WeeklyReport report = reportRepository.findById(id).orElseThrow(() -> BusinessException.notFound("周进展不存在"));
        boolean allowed = actor.getRole() == UserRole.ADMIN
                || actor.getId().equals(report.getStudentId())
                || actor.getId().equals(report.getMentorId());
        if (!allowed) throw BusinessException.forbidden("无权查看该周进展");
        return report;
    }

    public java.util.List<WeeklyReport> reportsForAi(String studentId) {
        User actor = currentUserService.require();
        String target = actor.getRole() == UserRole.STUDENT ? actor.getId() : studentId;
        if (actor.getRole() == UserRole.MENTOR) {
            User student = userRepository.findById(target).orElseThrow(() -> BusinessException.notFound("学生不存在"));
            if (!actor.getId().equals(student.getMentorId())) throw BusinessException.forbidden("该学生不属于当前导师");
        }
        if (actor.getRole() == UserRole.ADMIN) throw BusinessException.forbidden("管理员AI不读取学生进展");
        return reportRepository.findByStudentId(target, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "weekStart"))).getContent();
    }

    private PageRequest pageable(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "weekStart"));
    }

    private WeeklyReport ownedReport(String id, String studentId) {
        WeeklyReport report = reportRepository.findById(id).orElseThrow(() -> BusinessException.notFound("周进展不存在"));
        if (!studentId.equals(report.getStudentId())) throw BusinessException.forbidden("无权操作该周进展");
        return report;
    }

    private User requireStudent() {
        User user = currentUserService.require();
        if (user.getRole() != UserRole.STUDENT) throw BusinessException.forbidden("仅学生可提交周进展");
        return user;
    }

    private void validate(ReportCommand command) {
        if (command.weekStart().getDayOfWeek() != DayOfWeek.MONDAY) throw BusinessException.badRequest("weekStart 必须是星期一");
        if (command.progressPercentage() < 0 || command.progressPercentage() > 100) {
            throw BusinessException.badRequest("完成度必须在0到100之间");
        }
    }

    public record ReportCommand(LocalDate weekStart, String completedWork, String currentProblems,
                                String nextWeekPlan, int progressPercentage) {}
}
