package com.projecthelper.task;

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

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public Task create(TaskCommand command) {
        User actor = currentUserService.require();
        User student = resolveTargetStudent(actor, command.studentId());
        GraduationProject project = projectRepository.findByStudentId(student.getId())
                .orElseThrow(() -> BusinessException.badRequest("The target student has not created a graduation project"));
        Instant now = Instant.now();
        Task task = Task.builder().projectId(project.getId()).studentId(student.getId()).creatorId(actor.getId())
                .title(command.title().trim()).description(command.description()).type(command.type())
                .priority(command.priority()).status(TaskStatus.TODO).deadlineAt(command.deadlineAt())
                .createdAt(now).updatedAt(now).build();
        return taskRepository.save(task);
    }

    public Task createFromAi(String studentName, String title, String description, TaskType type,
                             TaskPriority priority, Instant deadlineAt) {
        User actor = currentUserService.require();
        String targetId;
        if (actor.getRole() == UserRole.STUDENT) {
            targetId = actor.getId();
        } else if (actor.getRole() == UserRole.MENTOR) {
            if (studentName == null || studentName.isBlank()) throw BusinessException.badRequest("Specify which student should receive the task");
            List<User> matches = userRepository.findByMentorIdAndRealName(actor.getId(), studentName.trim());
            if (matches.isEmpty()) throw BusinessException.notFound("No assigned student found: " + studentName);
            if (matches.size() > 1) throw BusinessException.conflict("Multiple students have that name; select a specific student");
            targetId = matches.getFirst().getId();
        } else {
            throw BusinessException.forbidden("Administrators cannot create student tasks through AI");
        }
        return create(new TaskCommand(targetId, title, description, type, priority, deadlineAt));
    }

    public Task update(String id, TaskCommand command) {
        User actor = currentUserService.require();
        Task task = get(id);
        if (!actor.getId().equals(task.getCreatorId())) throw BusinessException.forbidden("Only the creator can edit a task");
        if (task.getStatus() == TaskStatus.COMPLETED) throw BusinessException.conflict("Completed tasks cannot be edited");
        User student = resolveTargetStudent(actor, command.studentId());
        task.setStudentId(student.getId());
        projectRepository.findByStudentId(student.getId()).ifPresent(project -> task.setProjectId(project.getId()));
        task.setTitle(command.title().trim());
        task.setDescription(command.description());
        task.setType(command.type());
        task.setPriority(command.priority());
        task.setDeadlineAt(command.deadlineAt());
        task.setUpdatedAt(Instant.now());
        return taskRepository.save(task);
    }

    public void delete(String id) {
        User actor = currentUserService.require();
        Task task = get(id);
        if (!actor.getId().equals(task.getCreatorId())) throw BusinessException.forbidden("Only the creator can delete a task");
        taskRepository.delete(task);
    }

    public Task changeStatus(String id, TaskStatus status) {
        User actor = currentUserService.require();
        Task task = get(id);
        if (actor.getRole() != UserRole.STUDENT || !actor.getId().equals(task.getStudentId())) {
            throw BusinessException.forbidden("Only the assigned student can update task status");
        }
        task.setStatus(status);
        task.setCompletedAt(status == TaskStatus.COMPLETED ? Instant.now() : null);
        task.setUpdatedAt(Instant.now());
        return taskRepository.save(task);
    }

    public Page<Task> list(String studentId, TaskStatus status, int page, int size) {
        User actor = currentUserService.require();
        User student = resolveTargetStudent(actor, studentId);
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)),
                Sort.by(Sort.Direction.ASC, "deadlineAt"));
        return status == null ? taskRepository.findByStudentId(student.getId(), pageable)
                : taskRepository.findByStudentIdAndStatus(student.getId(), status, pageable);
    }

    public List<Task> listForAi(String studentName, TaskStatus status) {
        User actor = currentUserService.require();
        String targetId = null;
        if (actor.getRole() == UserRole.MENTOR && studentName != null && !studentName.isBlank()) {
            List<User> matches = userRepository.findByMentorIdAndRealName(actor.getId(), studentName.trim());
            if (matches.size() != 1) throw BusinessException.badRequest("Provide one unique assigned student name");
            targetId = matches.getFirst().getId();
        }
        return list(targetId, status, 0, 20).getContent();
    }

    public Task get(String id) {
        return taskRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Task not found"));
    }

    private User resolveTargetStudent(User actor, String requestedStudentId) {
        if (actor.getRole() == UserRole.STUDENT) return actor;
        if (actor.getRole() != UserRole.MENTOR) throw BusinessException.forbidden("Only students and mentors can access tasks");
        if (requestedStudentId == null || requestedStudentId.isBlank()) throw BusinessException.badRequest("Select a student");
        User student = userRepository.findById(requestedStudentId).orElseThrow(() -> BusinessException.notFound("Student not found"));
        if (student.getRole() != UserRole.STUDENT || !actor.getId().equals(student.getMentorId())) {
            throw BusinessException.forbidden("The student is not assigned to this mentor");
        }
        return student;
    }

    public record TaskCommand(String studentId, String title, String description, TaskType type,
                              TaskPriority priority, Instant deadlineAt) {}
}
