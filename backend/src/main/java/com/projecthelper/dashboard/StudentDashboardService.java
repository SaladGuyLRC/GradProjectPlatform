package com.projecthelper.dashboard;

import com.projecthelper.common.BusinessException;
import com.projecthelper.organization.OrganizationRepository;
import com.projecthelper.progress.WeeklyReport;
import com.projecthelper.progress.WeeklyReportRepository;
import com.projecthelper.project.GraduationProject;
import com.projecthelper.project.ProjectRepository;
import com.projecthelper.security.CurrentUserService;
import com.projecthelper.task.Task;
import com.projecthelper.task.TaskRepository;
import com.projecthelper.task.TaskStatus;
import com.projecthelper.user.User;
import com.projecthelper.user.UserRepository;
import com.projecthelper.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentDashboardService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final List<TaskStatus> OPEN = List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS);

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;
    private final WeeklyReportRepository reportRepository;
    private final TaskRepository taskRepository;

    public StudentDashboard dashboard() {
        User student = currentUserService.require();
        if (student.getRole() != UserRole.STUDENT) throw BusinessException.forbidden("The student dashboard is available only to students");
        User mentor = userRepository.findById(student.getMentorId()).orElse(null);
        String collegeName = organizationRepository.findById(student.getCollegeId()).map(unit -> unit.getName()).orElse("");
        String majorName = organizationRepository.findById(student.getMajorId()).map(unit -> unit.getName()).orElse("");
        GraduationProject project = projectRepository.findByStudentId(student.getId()).orElse(null);

        LocalDate monday = LocalDate.now(ZONE).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        WeeklyReport currentReport = reportRepository.findByStudentIdAndWeekStart(student.getId(), monday).orElse(null);

        Instant now = Instant.now();
        Instant threeDays = now.plus(Duration.ofDays(3));
        Instant sevenDays = now.plus(Duration.ofDays(7));
        List<Task> overdue = taskRepository.findByStudentIdAndStatusInAndDeadlineAtBeforeOrderByDeadlineAtAsc(student.getId(), OPEN, now);
        List<Task> nextThree = taskRepository.findByStudentIdAndStatusInAndDeadlineAtBetweenOrderByDeadlineAtAsc(student.getId(), OPEN, now, threeDays);
        List<Task> nextSeven = taskRepository.findByStudentIdAndStatusInAndDeadlineAtBetweenOrderByDeadlineAtAsc(student.getId(), OPEN, threeDays, sevenDays);

        return new StudentDashboard(
                new StudentProfile(student.getId(), student.getRealName(), student.getStudentNo(), collegeName, majorName),
                mentor == null ? null : new MentorProfile(mentor.getId(), mentor.getRealName(), mentor.getTeacherNo()),
                project, currentReport,
                new DeadlineBuckets(bucket(overdue), bucket(nextThree), bucket(nextSeven)));
    }

    private DeadlineBucket bucket(List<Task> tasks) {
        return new DeadlineBucket(tasks.size(), tasks.stream().limit(5).toList());
    }

    public record StudentDashboard(StudentProfile studentProfile, MentorProfile mentorProfile,
                                   GraduationProject project, WeeklyReport currentWeekReport,
                                   DeadlineBuckets deadlineBuckets) {}
    public record StudentProfile(String id, String realName, String studentNo, String college, String major) {}
    public record MentorProfile(String id, String realName, String teacherNo) {}
    public record DeadlineBuckets(DeadlineBucket overdue, DeadlineBucket nextThreeDays, DeadlineBucket nextSevenDays) {}
    public record DeadlineBucket(int count, List<Task> items) {}
}
