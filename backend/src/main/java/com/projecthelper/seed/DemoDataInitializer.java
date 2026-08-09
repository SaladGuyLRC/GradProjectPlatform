package com.projecthelper.seed;

import com.projecthelper.organization.OrganizationRepository;
import com.projecthelper.organization.OrganizationType;
import com.projecthelper.organization.OrganizationUnit;
import com.projecthelper.progress.WeeklyReport;
import com.projecthelper.progress.WeeklyReportRepository;
import com.projecthelper.progress.WeeklyReportStatus;
import com.projecthelper.project.GraduationProject;
import com.projecthelper.project.ProjectRepository;
import com.projecthelper.project.ProjectStatus;
import com.projecthelper.task.Task;
import com.projecthelper.task.TaskPriority;
import com.projecthelper.task.TaskRepository;
import com.projecthelper.task.TaskStatus;
import com.projecthelper.task.TaskType;
import com.projecthelper.user.User;
import com.projecthelper.user.UserRepository;
import com.projecthelper.user.UserRole;
import com.projecthelper.user.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Order(20)
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final WeeklyReportRepository reportRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;
    private final SeedProperties properties;
    private final Clock clock;

    @Override
    public void run(ApplicationArguments args) {
        Instant now = Instant.now(clock);
        LocalDate today = LocalDate.now(clock);
        LocalDate currentMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        Organizations organizations = seedOrganizations(now);
        Users users = seedUsers(organizations, now);
        Projects projects = seedProjects(users, today, now);
        seedWeeklyReports(users, projects, currentMonday, now);
        seedTasks(users, projects, now);

        log.info("Demo seed data is ready: 2 colleges, 3 majors, 11 users, 5 projects, 7 weekly reports and 12 tasks");
    }

    private Organizations seedOrganizations(Instant now) {
        OrganizationUnit computing = organization(
                "seed-org-college-computing", "School of Computing", OrganizationType.COLLEGE, null, 10, now);
        OrganizationUnit ai = organization(
                "seed-org-college-ai", "School of Artificial Intelligence", OrganizationType.COLLEGE, null, 20, now);
        OrganizationUnit software = organization(
                "seed-org-major-software", "Software Engineering", OrganizationType.MAJOR, computing, 10, now);
        OrganizationUnit computerScience = organization(
                "seed-org-major-computer-science", "Computer Science", OrganizationType.MAJOR, computing, 20, now);
        OrganizationUnit artificialIntelligence = organization(
                "seed-org-major-ai", "Artificial Intelligence", OrganizationType.MAJOR, ai, 10, now);
        return new Organizations(computing, ai, software, computerScience, artificialIntelligence);
    }

    private OrganizationUnit organization(String id, String name, OrganizationType type,
                                          OrganizationUnit parent, int sortOrder, Instant now) {
        String parentId = parent == null ? null : parent.getId();
        return existing(
                organizationRepository.findById(id),
                () -> organizationRepository.findByTypeAndNameAndParentId(type, name, parentId),
                () -> organizationRepository.save(OrganizationUnit.builder()
                        .id(id)
                        .name(name)
                        .type(type)
                        .parentId(parentId)
                        .path(parent == null ? ":" : ":" + parentId + ":")
                        .level(parent == null ? 1 : 2)
                        .sortOrder(sortOrder)
                        .active(true)
                        .createdAt(now)
                        .updatedAt(now)
                        .build()));
    }

    private Users seedUsers(Organizations organizations, Instant now) {
        User admin = user("seed-user-admin", "admin", "Admin", UserRole.ADMIN,
                null, null, null, null, null, UserStatus.ACTIVE, now);

        User mentor1 = user("seed-user-mentor1", "mentor1", "Alex Morgan", UserRole.MENTOR,
                null, "T2026001", organizations.computing().getId(), organizations.software().getId(),
                null, UserStatus.ACTIVE, now);
        User mentor2 = user("seed-user-mentor2", "mentor2", "Jordan Lee", UserRole.MENTOR,
                null, "T2026002", organizations.computing().getId(), organizations.computerScience().getId(),
                null, UserStatus.ACTIVE, now);
        User mentor3 = user("seed-user-mentor3", "mentor3", "Taylor Smith", UserRole.MENTOR,
                null, "T2026003", organizations.ai().getId(), organizations.artificialIntelligence().getId(),
                null, UserStatus.ACTIVE, now);

        User student1 = user("seed-user-student1", "student1", "Chris Chen", UserRole.STUDENT,
                "S2026001", null, organizations.computing().getId(), organizations.software().getId(),
                mentor1.getId(), UserStatus.ACTIVE, now);
        User student2 = user("seed-user-student2", "student2", "Jamie Lin", UserRole.STUDENT,
                "S2026002", null, organizations.computing().getId(), organizations.software().getId(),
                mentor1.getId(), UserStatus.ACTIVE, now);
        User student3 = user("seed-user-student3", "student3", "Morgan Zhao", UserRole.STUDENT,
                "S2026003", null, organizations.computing().getId(), organizations.computerScience().getId(),
                mentor2.getId(), UserStatus.ACTIVE, now);
        User student4 = user("seed-user-student4", "student4", "Casey Sun", UserRole.STUDENT,
                "S2026004", null, organizations.computing().getId(), organizations.computerScience().getId(),
                mentor2.getId(), UserStatus.ACTIVE, now);
        User student5 = user("seed-user-student5", "student5", "Robin Zhou", UserRole.STUDENT,
                "S2026005", null, organizations.ai().getId(), organizations.artificialIntelligence().getId(),
                mentor3.getId(), UserStatus.ACTIVE, now);
        User student6 = user("seed-user-student6", "student6", "Avery Wu", UserRole.STUDENT,
                "S2026006", null, organizations.ai().getId(), organizations.artificialIntelligence().getId(),
                mentor3.getId(), UserStatus.ACTIVE, now);
        User student7 = user("seed-user-student7", "student7", "Disabled Test Account", UserRole.STUDENT,
                "S2026999", null, organizations.ai().getId(), organizations.artificialIntelligence().getId(),
                mentor3.getId(), UserStatus.DISABLED, now);

        return new Users(admin, mentor1, mentor2, mentor3,
                student1, student2, student3, student4, student5, student6, student7);
    }

    private User user(String id, String username, String realName, UserRole role,
                      String studentNo, String teacherNo, String collegeId, String majorId,
                      String mentorId, UserStatus status, Instant now) {
        return existing(
                userRepository.findById(id),
                () -> userRepository.findByUsername(username),
                () -> userRepository.save(User.builder()
                        .id(id)
                        .username(username)
                        .passwordHash(passwordEncoder.encode(properties.getDefaultPassword()))
                        .realName(realName)
                        .role(role)
                        .studentNo(studentNo)
                        .teacherNo(teacherNo)
                        .collegeId(collegeId)
                        .majorId(majorId)
                        .mentorId(mentorId)
                        .status(status)
                        .createdAt(now)
                        .updatedAt(now)
                        .build()));
    }

    private Projects seedProjects(Users users, LocalDate today, Instant now) {
        GraduationProject project1 = project(
                "seed-project-student1", users.student1(),
                "ProjectHelper Graduation Project Collaboration and AI Assistant",
                "A graduation project collaboration system for students and mentors with weekly reports, tasks, a public knowledge base and an AI assistant.",
                List.of("Java 21", "Spring Boot", "MongoDB", "Redis Stack", "Vue 3"),
                "https://github.com/SaladGuyLRC/GradProjectPlatform",
                ProjectStatus.IN_PROGRESS, today.minusDays(75), today.plusDays(45), now);
        GraduationProject project2 = project(
                "seed-project-student2", users.student2(),
                "Campus Room and Lab Booking System",
                "Provides room and laboratory booking with conflict detection for students and staff.",
                List.of("Java", "Spring Boot", "MongoDB", "Vue 3"),
                "https://example.com/demo/lab-booking",
                ProjectStatus.IN_PROGRESS, today.minusDays(60), today.plusDays(55), now);
        GraduationProject project3 = project(
                "seed-project-student3", users.student3(),
                "Online Practical Teaching Platform",
                "Manages practical courses, lab reports and instructor feedback.",
                List.of("Java", "Spring Boot", "Redis", "Vue 3"),
                "https://example.com/demo/experiment-platform",
                ProjectStatus.PAUSED, today.minusDays(90), today.plusDays(35), now);
        GraduationProject project4 = project(
                "seed-project-student4", users.student4(),
                "Campus Marketplace",
                "Supports listing, searching and managing the status of second-hand campus goods.",
                List.of("Java", "MongoDB", "Vue 3"),
                "https://example.com/demo/campus-market",
                ProjectStatus.COMPLETED, today.minusDays(150), today.minusDays(10), now);
        GraduationProject project6 = project(
                "seed-project-student6", users.student6(),
                "Smart Thesis Formatting Assistant",
                "Checks chapters, citations and formatting against university thesis guidelines.",
                List.of("Java", "Spring AI", "MongoDB", "Vue 3"),
                "https://example.com/demo/thesis-assistant",
                ProjectStatus.IN_PROGRESS, today.minusDays(45), today.plusDays(70), now);
        return new Projects(project1, project2, project3, project4, project6);
    }

    private GraduationProject project(String id, User student, String title, String summary,
                                      List<String> techStack, String repositoryUrl, ProjectStatus status,
                                      LocalDate startDate, LocalDate plannedEndDate, Instant now) {
        return existing(
                projectRepository.findById(id),
                () -> projectRepository.findByStudentId(student.getId()),
                () -> projectRepository.save(GraduationProject.builder()
                        .id(id)
                        .studentId(student.getId())
                        .mentorId(student.getMentorId())
                        .title(title)
                        .summary(summary)
                        .techStack(techStack)
                        .repositoryUrl(repositoryUrl)
                        .status(status)
                        .startDate(startDate)
                        .plannedEndDate(plannedEndDate)
                        .createdAt(now)
                        .updatedAt(now)
                        .build()));
    }

    private void seedWeeklyReports(Users users, Projects projects, LocalDate currentMonday, Instant now) {
        weeklyReport("seed-report-student1-current", users.student1(), projects.project1(),
                currentMonday, WeeklyReportStatus.DRAFT,
                "Completed backend APIs for login, organization management, projects, weekly reports, tasks and the student dashboard.",
                "Redis Stack vector search and model tool calls still need validation in a real environment.",
                "Complete the AI conversation loop and start the Vue frontend foundation.", null, null, now);
        weeklyReport("seed-report-student1-previous", users.student1(), projects.project1(),
                currentMonday.minusWeeks(1), WeeklyReportStatus.REVIEWED,
                "Completed JWT authentication, college and major management, and the members tree.",
                "More object-level permission test scenarios are needed.",
                "Implement graduation projects, weekly reports and mentor reviews.",
                now.minus(Duration.ofDays(7)), "Progress is on track. Prioritise validating critical permission boundaries next.", now);

        weeklyReport("seed-report-student2-current", users.student2(), projects.project2(),
                currentMonday, WeeklyReportStatus.SUBMITTED,
                "Completed booking conflict detection and paginated booking queries.",
                "Time-slot conflict validation still needs testing under high concurrency.",
                "Add API tests and complete the first booking page.",
                now.minus(Duration.ofHours(5)), null, now);
        weeklyReport("seed-report-student2-previous", users.student2(), projects.project2(),
                currentMonday.minusWeeks(1), WeeklyReportStatus.REVIEWED,
                "Completed the base data model for laboratories and rooms.",
                "Booking rules need further confirmation with the mentor.",
                "Implement booking creation, cancellation and status management.",
                now.minus(Duration.ofDays(7)), "The data model is clear. Keep time and timezone handling consistent.", now);

        weeklyReport("seed-report-student3-current", users.student3(), projects.project3(),
                currentMonday, WeeklyReportStatus.SUBMITTED,
                "Completed practical course and student group design.",
                "Milestones need adjustment while the project is paused.",
                "Confirm the reduced scope and resume core API development.",
                now.minus(Duration.ofHours(8)), null, now);
        weeklyReport("seed-report-student4-previous", users.student4(), projects.project4(),
                currentMonday.minusWeeks(1), WeeklyReportStatus.REVIEWED,
                "Completed final tests, demo data and deployment documentation.",
                "No blocking issues.",
                "Prepare defense materials and the project summary.",
                now.minus(Duration.ofDays(7)), "The project has reached its target and can move into defense preparation.", now);
        weeklyReport("seed-report-student6-current", users.student6(), projects.project6(),
                currentMonday, WeeklyReportStatus.DRAFT,
                "Completed the thesis template parsing approach and chapter structure model.",
                "Text extraction quality varies significantly between PDFs.",
                "Implement formatting rule checks and result presentation.",
                null, null, now);
    }

    private WeeklyReport weeklyReport(String id, User student, GraduationProject project, LocalDate weekStart,
                                      WeeklyReportStatus status, String completedWork,
                                      String currentProblems, String nextWeekPlan, Instant submittedAt,
                                      String reviewContent, Instant now) {
        return existing(
                reportRepository.findById(id),
                () -> reportRepository.findByStudentIdAndWeekStart(student.getId(), weekStart),
                () -> {
                    WeeklyReport.Review review = reviewContent == null ? null : WeeklyReport.Review.builder()
                            .mentorId(student.getMentorId())
                            .content(reviewContent)
                            .reviewedAt(submittedAt.plus(Duration.ofHours(12)))
                            .updatedAt(submittedAt.plus(Duration.ofHours(12)))
                            .build();
                    Instant createdAt = submittedAt == null
                            ? now.minus(Duration.ofHours(1))
                            : submittedAt.minus(Duration.ofHours(2));
                    Instant updatedAt = review != null
                            ? review.getUpdatedAt()
                            : submittedAt == null ? now : submittedAt;
                    return reportRepository.save(WeeklyReport.builder()
                            .id(id)
                            .studentId(student.getId())
                            .mentorId(student.getMentorId())
                            .projectId(project.getId())
                            .weekStart(weekStart)
                            .weekEnd(weekStart.plusDays(6))
                            .completedWork(completedWork)
                            .currentProblems(currentProblems)
                            .nextWeekPlan(nextWeekPlan)
                            .status(status)
                            .submittedAt(submittedAt)
                            .review(review)
                            .createdAt(createdAt)
                            .updatedAt(updatedAt)
                            .build());
                });
    }

    private void seedTasks(Users users, Projects projects, Instant now) {
        task("seed-task-student1-overdue", users.student1(), users.mentor1(), projects.project1(),
                "Complete initial design documentation", "Organise requirements, architecture, data model and page prototypes.",
                TaskType.DOCUMENT, TaskPriority.HIGH, TaskStatus.TODO,
                now.minus(Duration.ofDays(2)), null, now);
        task("seed-task-student1-code", users.student1(), users.student1(), projects.project1(),
                "Complete Redis integration tests", "Validate RediSearch index creation and vector query responses.",
                TaskType.CODE, TaskPriority.HIGH, TaskStatus.IN_PROGRESS,
                now.plus(Duration.ofDays(1)), null, now);
        task("seed-task-student1-meeting", users.student1(), users.mentor1(), projects.project1(),
                "Attend the mentor progress meeting", "Demonstrate current APIs and confirm the frontend implementation order.",
                TaskType.MEETING, TaskPriority.MEDIUM, TaskStatus.TODO,
                now.plus(Duration.ofDays(2)), null, now);
        task("seed-task-student1-progress", users.student1(), users.student1(), projects.project1(),
                "Complete the first frontend release", "Complete the login, student dashboard and weekly reports pages.",
                TaskType.PROGRESS, TaskPriority.MEDIUM, TaskStatus.TODO,
                now.plus(Duration.ofDays(5)), null, now);
        task("seed-task-student1-experiment", users.student1(), users.mentor1(), projects.project1(),
                "Validate Qwen tool calls", "Test project, weekly report and task queries and task creation.",
                TaskType.EXPERIMENT, TaskPriority.LOW, TaskStatus.IN_PROGRESS,
                now.plus(Duration.ofDays(6)), null, now);
        task("seed-task-student1-completed", users.student1(), users.student1(), projects.project1(),
                "Initialise the Git repository", "Create the main, develop and feature branches.",
                TaskType.OTHER, TaskPriority.LOW, TaskStatus.COMPLETED,
                now.minus(Duration.ofDays(7)), now.minus(Duration.ofDays(6)), now);

        task("seed-task-student2-code", users.student2(), users.student2(), projects.project2(),
                "Integrate the booking page APIs", "Integrate booking creation and conflict messages.",
                TaskType.CODE, TaskPriority.HIGH, TaskStatus.TODO,
                now.plus(Duration.ofDays(4)), null, now);
        task("seed-task-student2-meeting", users.student2(), users.mentor1(), projects.project2(),
                "Confirm the booking scope", "Confirm with the mentor that real-time notifications are out of scope for the first release.",
                TaskType.MEETING, TaskPriority.MEDIUM, TaskStatus.COMPLETED,
                now.minus(Duration.ofDays(10)), now.minus(Duration.ofDays(9)), now);
        task("seed-task-student3-document", users.student3(), users.mentor2(), projects.project3(),
                "Complete the practical platform data model", "Organise the relationships between courses, labs, groups and reports.",
                TaskType.DOCUMENT, TaskPriority.HIGH, TaskStatus.IN_PROGRESS,
                now.plus(Duration.ofDays(2)), null, now);
        task("seed-task-student4-progress", users.student4(), users.student4(), projects.project4(),
                "Prepare project acceptance materials", "Archive test results, deployment notes and demo screenshots.",
                TaskType.PROGRESS, TaskPriority.LOW, TaskStatus.COMPLETED,
                now.minus(Duration.ofDays(20)), now.minus(Duration.ofDays(18)), now);
        task("seed-task-student6-experiment", users.student6(), users.mentor3(), projects.project6(),
                "Validate thesis template parsing", "Compare parsing results from three differently formatted PDFs.",
                TaskType.EXPERIMENT, TaskPriority.MEDIUM, TaskStatus.TODO,
                now.plus(Duration.ofDays(6)), null, now);
        task("seed-task-student6-other", users.student6(), users.student6(), projects.project6(),
                "Prepare citation format examples", "Prepare test examples for the GB/T 7714 citation format.",
                TaskType.OTHER, TaskPriority.LOW, TaskStatus.TODO,
                now.plus(Duration.ofDays(8)), null, now);
    }

    private Task task(String id, User student, User creator, GraduationProject project,
                      String title, String description, TaskType type, TaskPriority priority,
                      TaskStatus status, Instant deadlineAt, Instant completedAt, Instant now) {
        return taskRepository.findById(id).orElseGet(() -> {
            Instant createdAt = deadlineAt.isBefore(now)
                    ? deadlineAt.minus(Duration.ofDays(2))
                    : now.minus(Duration.ofDays(1));
            return taskRepository.save(Task.builder()
                    .id(id)
                    .projectId(project.getId())
                    .studentId(student.getId())
                    .creatorId(creator.getId())
                    .title(title)
                    .description(description)
                    .type(type)
                    .priority(priority)
                    .status(status)
                    .deadlineAt(deadlineAt)
                    .completedAt(completedAt)
                    .createdAt(createdAt)
                    .updatedAt(completedAt == null ? now : completedAt)
                    .build());
        });
    }

    private <T> T existing(Optional<T> byStableId, Supplier<Optional<T>> byBusinessKey, Supplier<T> create) {
        return byStableId.orElseGet(() -> byBusinessKey.get().orElseGet(create));
    }

    private record Organizations(OrganizationUnit computing, OrganizationUnit ai,
                                 OrganizationUnit software, OrganizationUnit computerScience,
                                 OrganizationUnit artificialIntelligence) {}

    private record Users(User admin, User mentor1, User mentor2, User mentor3,
                         User student1, User student2, User student3, User student4,
                         User student5, User student6, User student7) {}

    private record Projects(GraduationProject project1, GraduationProject project2,
                            GraduationProject project3, GraduationProject project4,
                            GraduationProject project6) {}
}
