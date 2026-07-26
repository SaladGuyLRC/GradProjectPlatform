package com.projecthelper.seed;

import com.projecthelper.organization.OrganizationRepository;
import com.projecthelper.organization.OrganizationType;
import com.projecthelper.organization.OrganizationUnit;
import com.projecthelper.progress.WeeklyReport;
import com.projecthelper.progress.WeeklyReportRepository;
import com.projecthelper.progress.WeeklyReportStatus;
import com.projecthelper.project.GraduationProject;
import com.projecthelper.project.ProjectRepository;
import com.projecthelper.task.Task;
import com.projecthelper.task.TaskPriority;
import com.projecthelper.task.TaskRepository;
import com.projecthelper.task.TaskStatus;
import com.projecthelper.task.TaskType;
import com.projecthelper.user.User;
import com.projecthelper.user.UserRepository;
import com.projecthelper.user.UserRole;
import com.projecthelper.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoDataInitializerTest {
    private static final Instant NOW = Instant.parse("2026-07-26T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("Asia/Shanghai"));

    @Mock private OrganizationRepository organizationRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private WeeklyReportRepository reportRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private final Map<String, OrganizationUnit> organizations = new LinkedHashMap<>();
    private final Map<String, User> users = new LinkedHashMap<>();
    private final Map<String, GraduationProject> projects = new LinkedHashMap<>();
    private final Map<String, WeeklyReport> reports = new LinkedHashMap<>();
    private final Map<String, Task> tasks = new LinkedHashMap<>();

    private DemoDataInitializer initializer;

    @BeforeEach
    void setUp() {
        SeedProperties properties = new SeedProperties();
        properties.setEnabled(true);
        properties.setDefaultPassword("ProjectHelper@123");

        when(passwordEncoder.encode(anyString())).thenReturn("encoded-demo-password");

        when(organizationRepository.findById(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(organizations.get(invocation.getArgument(0))));
        when(organizationRepository.findByTypeAndNameAndParentId(
                any(OrganizationType.class), anyString(), nullable(String.class)))
                .thenAnswer(invocation -> organizations.values().stream()
                        .filter(unit -> unit.getType() == invocation.getArgument(0))
                        .filter(unit -> unit.getName().equals(invocation.getArgument(1)))
                        .filter(unit -> Objects.equals(unit.getParentId(), invocation.getArgument(2)))
                        .findFirst());
        when(organizationRepository.save(any(OrganizationUnit.class))).thenAnswer(invocation -> {
            OrganizationUnit unit = invocation.getArgument(0);
            organizations.put(unit.getId(), unit);
            return unit;
        });

        when(userRepository.findById(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(users.get(invocation.getArgument(0))));
        when(userRepository.findByUsername(anyString()))
                .thenAnswer(invocation -> users.values().stream()
                        .filter(user -> user.getUsername().equals(invocation.getArgument(0)))
                        .findFirst());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            users.put(user.getId(), user);
            return user;
        });

        when(projectRepository.findById(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(projects.get(invocation.getArgument(0))));
        when(projectRepository.findByStudentId(anyString()))
                .thenAnswer(invocation -> projects.values().stream()
                        .filter(project -> project.getStudentId().equals(invocation.getArgument(0)))
                        .findFirst());
        when(projectRepository.save(any(GraduationProject.class))).thenAnswer(invocation -> {
            GraduationProject project = invocation.getArgument(0);
            projects.put(project.getId(), project);
            return project;
        });

        when(reportRepository.findById(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(reports.get(invocation.getArgument(0))));
        when(reportRepository.findByStudentIdAndWeekStart(anyString(), any(LocalDate.class)))
                .thenAnswer(invocation -> reports.values().stream()
                        .filter(report -> report.getStudentId().equals(invocation.getArgument(0)))
                        .filter(report -> report.getWeekStart().equals(invocation.getArgument(1)))
                        .findFirst());
        when(reportRepository.save(any(WeeklyReport.class))).thenAnswer(invocation -> {
            WeeklyReport report = invocation.getArgument(0);
            reports.put(report.getId(), report);
            return report;
        });

        when(taskRepository.findById(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(tasks.get(invocation.getArgument(0))));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            tasks.put(task.getId(), task);
            return task;
        });

        initializer = new DemoDataInitializer(
                organizationRepository, userRepository, projectRepository,
                reportRepository, taskRepository, passwordEncoder, properties, CLOCK);
    }

    @Test
    void createsCompleteIdempotentDatasetAndPreservesExistingBusinessRecords() throws Exception {
        OrganizationUnit existingCollege = OrganizationUnit.builder()
                .id("existing-computing")
                .name("计算机学院")
                .type(OrganizationType.COLLEGE)
                .path(":")
                .level(1)
                .active(true)
                .createdAt(NOW.minusSeconds(3600))
                .updatedAt(NOW.minusSeconds(3600))
                .build();
        organizations.put(existingCollege.getId(), existingCollege);

        User existingAdmin = User.builder()
                .id("existing-admin")
                .username("admin")
                .passwordHash("custom-password")
                .realName("自定义管理员")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .createdAt(NOW.minusSeconds(3600))
                .updatedAt(NOW.minusSeconds(3600))
                .build();
        users.put(existingAdmin.getId(), existingAdmin);

        initializer.run(null);
        initializer.run(null);

        assertEquals(5, organizations.size());
        assertEquals(11, users.size());
        assertEquals(5, projects.size());
        assertEquals(7, reports.size());
        assertEquals(12, tasks.size());

        assertSame(existingAdmin, users.get("existing-admin"));
        assertEquals("custom-password", users.get("existing-admin").getPasswordHash());
        assertTrue(organizations.values().stream()
                .filter(unit -> unit.getType() == OrganizationType.MAJOR)
                .filter(unit -> Set.of("软件工程", "计算机科学与技术").contains(unit.getName()))
                .allMatch(unit -> existingCollege.getId().equals(unit.getParentId())));

        User disabled = users.values().stream()
                .filter(user -> "student7".equals(user.getUsername()))
                .findFirst().orElseThrow();
        assertEquals(UserStatus.DISABLED, disabled.getStatus());

        User noProjectStudent = users.values().stream()
                .filter(user -> "student5".equals(user.getUsername()))
                .findFirst().orElseThrow();
        assertTrue(projects.values().stream()
                .noneMatch(project -> project.getStudentId().equals(noProjectStudent.getId())));

        assertEquals(Set.of(WeeklyReportStatus.DRAFT, WeeklyReportStatus.SUBMITTED, WeeklyReportStatus.REVIEWED),
                reports.values().stream().map(WeeklyReport::getStatus).collect(Collectors.toSet()));
        assertEquals(Set.of(TaskType.values()),
                tasks.values().stream().map(Task::getType).collect(Collectors.toSet()));
        assertEquals(Set.of(TaskPriority.values()),
                tasks.values().stream().map(Task::getPriority).collect(Collectors.toSet()));
        assertEquals(Set.of(TaskStatus.values()),
                tasks.values().stream().map(Task::getStatus).collect(Collectors.toSet()));
        assertTrue(reports.values().stream()
                .allMatch(report -> !report.getCreatedAt().isAfter(report.getUpdatedAt())));
        assertTrue(reports.values().stream()
                .filter(report -> report.getSubmittedAt() != null)
                .allMatch(report -> !report.getCreatedAt().isAfter(report.getSubmittedAt())));
        assertTrue(tasks.values().stream()
                .allMatch(task -> !task.getCreatedAt().isAfter(task.getDeadlineAt())));
        assertTrue(tasks.values().stream()
                .filter(task -> task.getCompletedAt() != null)
                .allMatch(task -> !task.getCreatedAt().isAfter(task.getCompletedAt())));
    }

    @Test
    void createsDashboardDeadlineBucketsForPrimaryStudent() throws Exception {
        initializer.run(null);

        User student = users.values().stream()
                .filter(user -> "student1".equals(user.getUsername()))
                .findFirst().orElseThrow();
        List<Task> openTasks = tasks.values().stream()
                .filter(task -> task.getStudentId().equals(student.getId()))
                .filter(task -> task.getStatus() != TaskStatus.COMPLETED)
                .toList();

        long overdue = openTasks.stream()
                .filter(task -> task.getDeadlineAt().isBefore(NOW))
                .count();
        long nextThree = openTasks.stream()
                .filter(task -> !task.getDeadlineAt().isBefore(NOW))
                .filter(task -> !task.getDeadlineAt().isAfter(NOW.plus(Duration.ofDays(3))))
                .count();
        long nextSeven = openTasks.stream()
                .filter(task -> task.getDeadlineAt().isAfter(NOW.plus(Duration.ofDays(3))))
                .filter(task -> !task.getDeadlineAt().isAfter(NOW.plus(Duration.ofDays(7))))
                .count();

        assertEquals(1, overdue);
        assertEquals(2, nextThree);
        assertEquals(2, nextSeven);
    }
}
