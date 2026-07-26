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
                "seed-org-college-computing", "计算机学院", OrganizationType.COLLEGE, null, 10, now);
        OrganizationUnit ai = organization(
                "seed-org-college-ai", "人工智能学院", OrganizationType.COLLEGE, null, 20, now);
        OrganizationUnit software = organization(
                "seed-org-major-software", "软件工程", OrganizationType.MAJOR, computing, 10, now);
        OrganizationUnit computerScience = organization(
                "seed-org-major-computer-science", "计算机科学与技术", OrganizationType.MAJOR, computing, 20, now);
        OrganizationUnit artificialIntelligence = organization(
                "seed-org-major-ai", "人工智能", OrganizationType.MAJOR, ai, 10, now);
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
        User admin = user("seed-user-admin", "admin", "系统管理员", UserRole.ADMIN,
                null, null, null, null, null, UserStatus.ACTIVE, now);

        User mentor1 = user("seed-user-mentor1", "mentor1", "王建国", UserRole.MENTOR,
                null, "T2026001", organizations.computing().getId(), organizations.software().getId(),
                null, UserStatus.ACTIVE, now);
        User mentor2 = user("seed-user-mentor2", "mentor2", "李明", UserRole.MENTOR,
                null, "T2026002", organizations.computing().getId(), organizations.computerScience().getId(),
                null, UserStatus.ACTIVE, now);
        User mentor3 = user("seed-user-mentor3", "mentor3", "张敏", UserRole.MENTOR,
                null, "T2026003", organizations.ai().getId(), organizations.artificialIntelligence().getId(),
                null, UserStatus.ACTIVE, now);

        User student1 = user("seed-user-student1", "student1", "陈宇", UserRole.STUDENT,
                "S2026001", null, organizations.computing().getId(), organizations.software().getId(),
                mentor1.getId(), UserStatus.ACTIVE, now);
        User student2 = user("seed-user-student2", "student2", "林曦", UserRole.STUDENT,
                "S2026002", null, organizations.computing().getId(), organizations.software().getId(),
                mentor1.getId(), UserStatus.ACTIVE, now);
        User student3 = user("seed-user-student3", "student3", "赵磊", UserRole.STUDENT,
                "S2026003", null, organizations.computing().getId(), organizations.computerScience().getId(),
                mentor2.getId(), UserStatus.ACTIVE, now);
        User student4 = user("seed-user-student4", "student4", "孙悦", UserRole.STUDENT,
                "S2026004", null, organizations.computing().getId(), organizations.computerScience().getId(),
                mentor2.getId(), UserStatus.ACTIVE, now);
        User student5 = user("seed-user-student5", "student5", "周涵", UserRole.STUDENT,
                "S2026005", null, organizations.ai().getId(), organizations.artificialIntelligence().getId(),
                mentor3.getId(), UserStatus.ACTIVE, now);
        User student6 = user("seed-user-student6", "student6", "吴佳", UserRole.STUDENT,
                "S2026006", null, organizations.ai().getId(), organizations.artificialIntelligence().getId(),
                mentor3.getId(), UserStatus.ACTIVE, now);
        User student7 = user("seed-user-student7", "student7", "停用测试账号", UserRole.STUDENT,
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
                "ProjectHelper 毕业设计协作与 AI 助手系统",
                "面向学生和导师的毕业设计过程协作系统，支持周进展、待办、公共知识库和 AI 助手。",
                List.of("Java 21", "Spring Boot", "MongoDB", "Redis Stack", "Vue 3"),
                "https://github.com/SaladGuyLRC/ProjectHelper1",
                ProjectStatus.IN_PROGRESS, today.minusDays(75), today.plusDays(45), now);
        GraduationProject project2 = project(
                "seed-project-student2", users.student2(),
                "校园会议与实验室预约系统",
                "为学生和教师提供会议室、实验室预约及冲突检测。",
                List.of("Java", "Spring Boot", "MongoDB", "Vue 3"),
                "https://example.com/demo/lab-booking",
                ProjectStatus.IN_PROGRESS, today.minusDays(60), today.plusDays(55), now);
        GraduationProject project3 = project(
                "seed-project-student3", users.student3(),
                "在线实验教学管理平台",
                "管理实验课程、实验报告和教师反馈。",
                List.of("Java", "Spring Boot", "Redis", "Vue 3"),
                "https://example.com/demo/experiment-platform",
                ProjectStatus.PAUSED, today.minusDays(90), today.plusDays(35), now);
        GraduationProject project4 = project(
                "seed-project-student4", users.student4(),
                "校园二手交易平台",
                "支持校园二手物品发布、搜索和交易状态管理。",
                List.of("Java", "MongoDB", "Vue 3"),
                "https://example.com/demo/campus-market",
                ProjectStatus.COMPLETED, today.minusDays(150), today.minusDays(10), now);
        GraduationProject project6 = project(
                "seed-project-student6", users.student6(),
                "毕业论文智能排版助手",
                "根据学校论文规范检查章节、引用和排版问题。",
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
                currentMonday, WeeklyReportStatus.DRAFT, 45,
                "完成登录、组织管理、项目、周进展、待办和学生 Dashboard 的后端接口。",
                "Redis Stack 向量检索和模型工具调用仍需真实环境验证。",
                "完成 AI 对话闭环并开始 Vue 前端基础框架。", null, null, now);
        weeklyReport("seed-report-student1-previous", users.student1(), projects.project1(),
                currentMonday.minusWeeks(1), WeeklyReportStatus.REVIEWED, 38,
                "完成 JWT 认证、学院专业管理和成员树。",
                "对象级权限测试场景需要进一步补充。",
                "实现毕设项目、周进展和导师评阅。",
                now.minus(Duration.ofDays(7)), "进度符合预期，下一阶段优先验证关键权限边界。", now);

        weeklyReport("seed-report-student2-current", users.student2(), projects.project2(),
                currentMonday, WeeklyReportStatus.SUBMITTED, 60,
                "完成预约冲突检测和预约记录分页查询。",
                "高并发场景下的时间段冲突校验还需测试。",
                "补充接口测试并完成第一版预约页面。",
                now.minus(Duration.ofHours(5)), null, now);
        weeklyReport("seed-report-student2-previous", users.student2(), projects.project2(),
                currentMonday.minusWeeks(1), WeeklyReportStatus.REVIEWED, 52,
                "完成实验室和会议室基础数据模型。",
                "预约规则需要与导师进一步确认。",
                "实现预约创建、取消和状态管理。",
                now.minus(Duration.ofDays(7)), "数据模型清晰，注意统一时间与时区处理。", now);

        weeklyReport("seed-report-student3-current", users.student3(), projects.project3(),
                currentMonday, WeeklyReportStatus.SUBMITTED, 40,
                "完成实验课程和学生分组设计。",
                "项目暂停期间需要重新调整里程碑。",
                "确认缩减范围并恢复核心接口开发。",
                now.minus(Duration.ofHours(8)), null, now);
        weeklyReport("seed-report-student4-previous", users.student4(), projects.project4(),
                currentMonday.minusWeeks(1), WeeklyReportStatus.REVIEWED, 100,
                "完成最终测试、演示数据和部署文档。",
                "无阻塞问题。",
                "整理答辩材料和项目总结。",
                now.minus(Duration.ofDays(7)), "项目已达到预期目标，可以进入答辩准备阶段。", now);
        weeklyReport("seed-report-student6-current", users.student6(), projects.project6(),
                currentMonday, WeeklyReportStatus.DRAFT, 25,
                "完成论文模板解析方案和章节结构模型。",
                "不同 PDF 的文本提取质量差异较大。",
                "实现格式规则检查和结果展示。",
                null, null, now);
    }

    private WeeklyReport weeklyReport(String id, User student, GraduationProject project, LocalDate weekStart,
                                      WeeklyReportStatus status, int progress, String completedWork,
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
                            .progressPercentage(progress)
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
                "补充前期设计文档", "整理需求、架构、数据模型和页面原型。",
                TaskType.DOCUMENT, TaskPriority.HIGH, TaskStatus.TODO,
                now.minus(Duration.ofDays(2)), null, now);
        task("seed-task-student1-code", users.student1(), users.student1(), projects.project1(),
                "完成 Redis 集成测试", "验证 RediSearch 索引创建和向量查询响应。",
                TaskType.CODE, TaskPriority.HIGH, TaskStatus.IN_PROGRESS,
                now.plus(Duration.ofDays(1)), null, now);
        task("seed-task-student1-meeting", users.student1(), users.mentor1(), projects.project1(),
                "参加导师进度会议", "演示当前接口并确认前端实现顺序。",
                TaskType.MEETING, TaskPriority.MEDIUM, TaskStatus.TODO,
                now.plus(Duration.ofDays(2)), null, now);
        task("seed-task-student1-progress", users.student1(), users.student1(), projects.project1(),
                "完成前端第一版", "完成登录、学生 Dashboard 和周进展页面。",
                TaskType.PROGRESS, TaskPriority.MEDIUM, TaskStatus.TODO,
                now.plus(Duration.ofDays(5)), null, now);
        task("seed-task-student1-experiment", users.student1(), users.mentor1(), projects.project1(),
                "验证 Qwen 工具调用", "测试查询项目、周报、待办和创建待办。",
                TaskType.EXPERIMENT, TaskPriority.LOW, TaskStatus.IN_PROGRESS,
                now.plus(Duration.ofDays(6)), null, now);
        task("seed-task-student1-completed", users.student1(), users.student1(), projects.project1(),
                "初始化 Git 仓库", "建立 main、develop 和 feature 分支。",
                TaskType.OTHER, TaskPriority.LOW, TaskStatus.COMPLETED,
                now.minus(Duration.ofDays(7)), now.minus(Duration.ofDays(6)), now);

        task("seed-task-student2-code", users.student2(), users.student2(), projects.project2(),
                "完成预约页面接口联调", "联调预约创建和冲突提示。",
                TaskType.CODE, TaskPriority.HIGH, TaskStatus.TODO,
                now.plus(Duration.ofDays(4)), null, now);
        task("seed-task-student2-meeting", users.student2(), users.mentor1(), projects.project2(),
                "确认预约业务范围", "与导师确认首期不做实时通知。",
                TaskType.MEETING, TaskPriority.MEDIUM, TaskStatus.COMPLETED,
                now.minus(Duration.ofDays(10)), now.minus(Duration.ofDays(9)), now);
        task("seed-task-student3-document", users.student3(), users.mentor2(), projects.project3(),
                "补充实验平台数据模型", "整理课程、实验、分组和报告关系。",
                TaskType.DOCUMENT, TaskPriority.HIGH, TaskStatus.IN_PROGRESS,
                now.plus(Duration.ofDays(2)), null, now);
        task("seed-task-student4-progress", users.student4(), users.student4(), projects.project4(),
                "整理项目验收材料", "归档测试结果、部署说明和演示截图。",
                TaskType.PROGRESS, TaskPriority.LOW, TaskStatus.COMPLETED,
                now.minus(Duration.ofDays(20)), now.minus(Duration.ofDays(18)), now);
        task("seed-task-student6-experiment", users.student6(), users.mentor3(), projects.project6(),
                "验证论文模板解析", "选择三份不同格式的 PDF 比较解析结果。",
                TaskType.EXPERIMENT, TaskPriority.MEDIUM, TaskStatus.TODO,
                now.plus(Duration.ofDays(6)), null, now);
        task("seed-task-student6-other", users.student6(), users.student6(), projects.project6(),
                "整理引用格式示例", "准备 GB/T 7714 引用格式测试样例。",
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
