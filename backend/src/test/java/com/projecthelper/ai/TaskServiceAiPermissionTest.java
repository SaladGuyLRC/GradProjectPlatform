package com.projecthelper.ai;

import com.projecthelper.common.BusinessException;
import com.projecthelper.project.GraduationProject;
import com.projecthelper.project.ProjectRepository;
import com.projecthelper.security.CurrentUserService;
import com.projecthelper.task.*;
import com.projecthelper.user.User;
import com.projecthelper.user.UserRepository;
import com.projecthelper.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class TaskServiceAiPermissionTest {
    @Mock TaskRepository taskRepository;
    @Mock ProjectRepository projectRepository;
    @Mock UserRepository userRepository;
    @Mock CurrentUserService currentUserService;

    private TaskService service;
    private User student;
    private User assignedStudent;
    private User mentor;

    @BeforeEach
    void setUp() {
        service = new TaskService(taskRepository, projectRepository, userRepository, currentUserService);
        student = User.builder().id("s1").realName("Alice").role(UserRole.STUDENT).build();
        assignedStudent = User.builder().id("s2").realName("Bob").mentorId("m1").role(UserRole.STUDENT).build();
        mentor = User.builder().id("m1").realName("Mentor").role(UserRole.MENTOR).build();
    }

    @Test
    void studentAiQueryIsAlwaysScopedToCurrentStudent() {
        when(currentUserService.require()).thenReturn(student);
        when(taskRepository.findByStudentId(any(), any())).thenReturn(new PageImpl<>(List.of()));

        service.listForAi(null, null);

        verify(taskRepository).findByStudentId(eq("s1"), org.mockito.ArgumentMatchers.any());
        service.listForAi("Bob", null);
        verify(taskRepository, org.mockito.Mockito.times(2)).findByStudentId(eq("s1"), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void mentorAiQueryCannotCrossMentorBoundary() {
        when(currentUserService.require()).thenReturn(mentor);
        when(userRepository.findByMentorIdAndRealName("m1", "Bob")).thenReturn(List.of(assignedStudent));
        when(userRepository.findById("s2")).thenReturn(Optional.of(assignedStudent));
        when(taskRepository.findByStudentId(any(), any())).thenReturn(new PageImpl<>(List.of()));

        service.listForAi("Bob", null);

        verify(taskRepository).findByStudentId(eq("s2"), org.mockito.ArgumentMatchers.any());
        when(userRepository.findByMentorIdAndRealName("m1", "Other")).thenReturn(List.of());
        assertThrows(BusinessException.class, () -> service.listForAi("Other", null));
    }

    @Test
    void studentAndMentorCanCreateOnlyAllowedTasks() {
        GraduationProject project = GraduationProject.builder().id("p1").studentId("s1").build();
        when(projectRepository.findByStudentId(any())).thenReturn(Optional.of(project));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(currentUserService.require()).thenReturn(student);
        Task studentTask = service.createFromAi(null, "Student task", "desc", TaskType.PROGRESS,
                TaskPriority.MEDIUM, Instant.now().plusSeconds(3600));
        assertEquals("s1", studentTask.getStudentId());

        when(currentUserService.require()).thenReturn(mentor);
        when(userRepository.findByMentorIdAndRealName("m1", "Bob")).thenReturn(List.of(assignedStudent));
        when(userRepository.findById("s2")).thenReturn(Optional.of(assignedStudent));
        when(projectRepository.findByStudentId("s2")).thenReturn(Optional.of(
                GraduationProject.builder().id("p2").studentId("s2").build()));
        Task mentorTask = service.createFromAi("Bob", "Mentor task", null, TaskType.MEETING,
                TaskPriority.HIGH, Instant.now().plusSeconds(3600));
        assertEquals("s2", mentorTask.getStudentId());

        User admin = User.builder().id("a1").role(UserRole.ADMIN).build();
        when(currentUserService.require()).thenReturn(admin);
        assertThrows(BusinessException.class, () -> service.createFromAi("Bob", "Denied", null,
                TaskType.OTHER, TaskPriority.LOW, Instant.now().plusSeconds(3600)));
    }
}
