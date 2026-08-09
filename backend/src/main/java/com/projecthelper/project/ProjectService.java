package com.projecthelper.project;

import com.projecthelper.common.BusinessException;
import com.projecthelper.progress.WeeklyReportRepository;
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
    private final WeeklyReportRepository weeklyReportRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public GraduationProject myProject() {
        User student = requireStudent();
        return projectRepository.findByStudentId(student.getId()).orElse(null);
    }

    public GraduationProject create(ProjectCommand command) {
        User student = requireStudent();
        if (projectRepository.findByStudentId(student.getId()).isPresent()) throw BusinessException.conflict("Each student can create only one graduation project");
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
                .orElseThrow(() -> BusinessException.notFound("Create a graduation project first"));
        if (!java.util.Objects.equals(project.getStartDate(), command.startDate())
                && weeklyReportRepository.findFirstByStudentIdOrderByWeekStartDesc(student.getId()).isPresent()) {
            throw BusinessException.conflict("The project start date cannot be changed after the first weekly report is created");
        }
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
        if (mentor.getRole() != UserRole.MENTOR) throw BusinessException.forbidden("Only mentors can view student projects");
        User student = requireSupervisedStudent(mentor, studentId);
        return projectRepository.findByStudentId(student.getId()).orElse(null);
    }

    public GraduationProject projectForActorStudent(String studentId) {
        User actor = currentUserService.require();
        String target = actor.getRole() == UserRole.STUDENT ? actor.getId() : requireSupervisedStudent(actor, studentId).getId();
        return projectRepository.findByStudentId(target).orElse(null);
    }

    public User requireSupervisedStudent(User mentor, String studentId) {
        if (mentor.getRole() != UserRole.MENTOR) throw BusinessException.forbidden("The current user is not a mentor");
        User student = userRepository.findById(studentId).orElseThrow(() -> BusinessException.notFound("Student not found"));
        if (student.getRole() != UserRole.STUDENT || !mentor.getId().equals(student.getMentorId())) {
            throw BusinessException.forbidden("The student is not assigned to this mentor");
        }
        return student;
    }

    private User requireStudent() {
        User user = currentUserService.require();
        if (user.getRole() != UserRole.STUDENT) throw BusinessException.forbidden("Only students can maintain their own graduation project");
        return user;
    }

    private void validate(ProjectCommand command) {
        if (command.startDate() != null && command.plannedEndDate() != null
                && command.plannedEndDate().isBefore(command.startDate())) {
            throw BusinessException.badRequest("The planned end date cannot be before the start date");
        }
    }

    public record ProjectCommand(String title, String summary, java.util.List<String> techStack,
                                 String repositoryUrl, ProjectStatus status,
                                 java.time.LocalDate startDate, java.time.LocalDate plannedEndDate) {}
}
