package com.projecthelper.project;

import com.projecthelper.common.BusinessException;
import com.projecthelper.security.CurrentUserService;
import com.projecthelper.user.User;
import com.projecthelper.user.UserRepository;
import com.projecthelper.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public GraduationProject myProject() {
        User student = requireStudent();
        return projectRepository.findByStudentId(student.getId()).orElse(null);
    }

    public GraduationProject create(ProjectCommand command) {
        User student = requireStudent();
        if (projectRepository.findByStudentId(student.getId()).isPresent()) throw BusinessException.conflict("每名学生只能创建一个毕设项目");
        validate(command);
        Instant now = Instant.now();
        return projectRepository.save(GraduationProject.builder().studentId(student.getId()).mentorId(student.getMentorId())
                .title(command.title().trim()).summary(command.summary()).techStack(command.techStack())
                .repositoryUrl(command.repositoryUrl()).status(command.status()).startDate(command.startDate())
                .plannedEndDate(command.plannedEndDate()).createdAt(now).updatedAt(now).build());
    }

    public GraduationProject update(ProjectCommand command) {
        User student = requireStudent();
        GraduationProject project = projectRepository.findByStudentId(student.getId())
                .orElseThrow(() -> BusinessException.notFound("请先创建毕设项目"));
        validate(command);
        project.setTitle(command.title().trim());
        project.setSummary(command.summary());
        project.setTechStack(command.techStack());
        project.setRepositoryUrl(command.repositoryUrl());
        project.setStatus(command.status());
        project.setStartDate(command.startDate());
        project.setPlannedEndDate(command.plannedEndDate());
        project.setUpdatedAt(Instant.now());
        return projectRepository.save(project);
    }

    public GraduationProject mentorView(String studentId) {
        User mentor = currentUserService.require();
        if (mentor.getRole() != UserRole.MENTOR) throw BusinessException.forbidden("仅导师可查看学生项目");
        User student = requireSupervisedStudent(mentor, studentId);
        return projectRepository.findByStudentId(student.getId()).orElse(null);
    }

    public GraduationProject projectForActorStudent(String studentId) {
        User actor = currentUserService.require();
        String target = actor.getRole() == UserRole.STUDENT ? actor.getId() : requireSupervisedStudent(actor, studentId).getId();
        return projectRepository.findByStudentId(target).orElse(null);
    }

    public User requireSupervisedStudent(User mentor, String studentId) {
        if (mentor.getRole() != UserRole.MENTOR) throw BusinessException.forbidden("当前用户不是导师");
        User student = userRepository.findById(studentId).orElseThrow(() -> BusinessException.notFound("学生不存在"));
        if (student.getRole() != UserRole.STUDENT || !mentor.getId().equals(student.getMentorId())) {
            throw BusinessException.forbidden("该学生不属于当前导师");
        }
        return student;
    }

    private User requireStudent() {
        User user = currentUserService.require();
        if (user.getRole() != UserRole.STUDENT) throw BusinessException.forbidden("仅学生可维护自己的毕设项目");
        return user;
    }

    private void validate(ProjectCommand command) {
        if (command.startDate() != null && command.plannedEndDate() != null
                && command.plannedEndDate().isBefore(command.startDate())) {
            throw BusinessException.badRequest("计划完成日期不能早于开始日期");
        }
    }

    public record ProjectCommand(String title, String summary, java.util.List<String> techStack,
                                 String repositoryUrl, ProjectStatus status,
                                 java.time.LocalDate startDate, java.time.LocalDate plannedEndDate) {}
}
