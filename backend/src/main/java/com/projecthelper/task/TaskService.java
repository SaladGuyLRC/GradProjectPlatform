package com.projecthelper.task;

import com.projecthelper.common.BusinessException;
import com.projecthelper.project.GraduationProject;
import com.projecthelper.project.ProjectRepository;
import com.projecthelper.security.CurrentUserService;
import com.projecthelper.user.User;
import com.projecthelper.user.UserRepository;
import com.projecthelper.user.UserRole;
import com.projecthelper.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskService {
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_DESCRIPTION_LENGTH = 4000;

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public Task create(TaskCommand command) {
        User actor = currentUserService.require();
        User student = resolveTargetStudent(actor, command.studentId());
        GraduationProject project = requireProject(student.getId());
        validateCommand(command);
        return saveTask(actor, student, project, command, null);
    }

    public AiTaskTarget resolveAiTarget(String studentName) {
        User actor = currentUserService.require();
        User student;
        if (actor.getRole() == UserRole.STUDENT) {
            student = actor;
        } else if (actor.getRole() == UserRole.MENTOR) {
            List<User> matches;
            if (studentName == null || studentName.isBlank()) {
                matches = userRepository.findByMentorIdOrderByRealName(actor.getId()).stream()
                        .filter(user -> user.getStatus() != UserStatus.DISABLED).toList();
                if (matches.size() != 1) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "ASSIGNEE_REQUIRED",
                            "Please specify which student this task is for.");
                }
            } else {
                matches = userRepository.findByMentorIdAndRealName(actor.getId(), studentName.trim()).stream()
                        .filter(user -> user.getStatus() != UserStatus.DISABLED).toList();
                if (matches.isEmpty()) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "ASSIGNEE_NOT_FOUND",
                            "No assigned student matches that name.");
                }
                if (matches.size() > 1) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "ASSIGNEE_AMBIGUOUS",
                            "More than one student matches that name. Please provide the student's number.");
                }
            }
            student = matches.getFirst();
        } else {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "Administrators cannot create student tasks through AI.");
        }
        requireActive(student);
        GraduationProject project = requireProject(student.getId());
        return new AiTaskTarget(student.getId(), project.getId(), student.getRealName());
    }

    public AiTaskTarget validateAiTarget(String studentId) {
        User actor = currentUserService.require();
        User student = resolveTargetStudent(actor, studentId);
        GraduationProject project = requireProject(student.getId());
        return new AiTaskTarget(student.getId(), project.getId(), student.getRealName());
    }

    public Optional<Task> findBySourceDraftId(String sourceDraftId) {
        return taskRepository.findBySourceDraftId(sourceDraftId);
    }

    public Task createFromDraft(String sourceDraftId, String studentId, String title, String description,
                                TaskType type, TaskPriority priority, Instant deadlineAt) {
        User actor = currentUserService.require();
        User student = resolveTargetStudent(actor, studentId);
        GraduationProject project = requireProject(student.getId());
        TaskCommand command = new TaskCommand(studentId, title, description, type, priority, deadlineAt);
        validateCommand(command);
        Optional<Task> existing = taskRepository.findBySourceDraftId(sourceDraftId);
        if (existing.isPresent()) return existing.get();
        try {
            return saveTask(actor, student, project, command, sourceDraftId);
        } catch (DuplicateKeyException exception) {
            return taskRepository.findBySourceDraftId(sourceDraftId).orElseThrow(() -> exception);
        }
    }

    public Task createFromAi(String studentName, String title, String description, TaskType type,
                             TaskPriority priority, Instant deadlineAt) {
        AiTaskTarget target = resolveAiTarget(studentName);
        return create(new TaskCommand(target.studentId(), title, description, type, priority, deadlineAt));
    }

    private Task saveTask(User actor, User student, GraduationProject project, TaskCommand command,
                          String sourceDraftId) {
        Instant now = Instant.now();
        Task task = Task.builder().projectId(project.getId()).studentId(student.getId()).creatorId(actor.getId())
                .sourceDraftId(sourceDraftId).title(command.title().trim())
                .description(normalizeDescription(command.description())).type(command.type())
                .priority(command.priority()).status(TaskStatus.TODO).deadlineAt(command.deadlineAt())
                .createdAt(now).updatedAt(now).build();
        return taskRepository.save(task);
    }

    public Task update(String id, TaskCommand command) {
        User actor = currentUserService.require();
        Task task = get(id);
        if (!actor.getId().equals(task.getCreatorId())) throw BusinessException.forbidden("Only the creator can edit a task");
        if (task.getStatus() == TaskStatus.COMPLETED) throw BusinessException.conflict("Completed tasks cannot be edited");
        User student = resolveTargetStudent(actor, command.studentId());
        validateCommand(command);
        task.setStudentId(student.getId());
        projectRepository.findByStudentId(student.getId()).ifPresent(project -> task.setProjectId(project.getId()));
        task.setTitle(command.title().trim());
        task.setDescription(normalizeDescription(command.description()));
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
        if (actor.getRole() == UserRole.STUDENT) {
            requireActive(actor);
            return actor;
        }
        if (actor.getRole() != UserRole.MENTOR) throw BusinessException.forbidden("Only students and mentors can access tasks");
        if (requestedStudentId == null || requestedStudentId.isBlank()) throw BusinessException.badRequest("Select a student");
        User student = userRepository.findById(requestedStudentId).orElseThrow(() -> BusinessException.notFound("Student not found"));
        if (student.getRole() != UserRole.STUDENT || !actor.getId().equals(student.getMentorId())) {
            throw BusinessException.forbidden("The student is not assigned to this mentor");
        }
        requireActive(student);
        return student;
    }

    private GraduationProject requireProject(String studentId) {
        return projectRepository.findByStudentId(studentId).orElseThrow(() ->
                new BusinessException(HttpStatus.BAD_REQUEST, "PROJECT_REQUIRED",
                        "A project is required before this task can be created."));
    }

    private void requireActive(User user) {
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "USER_DISABLED",
                    "A disabled user cannot create or receive tasks.");
        }
    }

    private void validateCommand(TaskCommand command) {
        if (command.title() == null || command.title().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TITLE_REQUIRED", "Please provide a title for the task.");
        }
        if (command.title().trim().length() > MAX_TITLE_LENGTH) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TITLE_TOO_LONG",
                    "The task title must be 200 characters or fewer.");
        }
        if (command.description() != null && command.description().trim().length() > MAX_DESCRIPTION_LENGTH) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "DESCRIPTION_TOO_LONG",
                    "The task description must be 4000 characters or fewer.");
        }
        if (command.type() == null || command.priority() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TASK_FIELDS_REQUIRED",
                    "Task type and priority are required.");
        }
        if (command.deadlineAt() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "DEADLINE_REQUIRED",
                    "Please provide a deadline for the task.");
        }
        if (!command.deadlineAt().isAfter(Instant.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "DEADLINE_IN_PAST",
                    "The deadline must be in the future.");
        }
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) return null;
        return description.trim();
    }

    public record TaskCommand(String studentId, String title, String description, TaskType type,
                              TaskPriority priority, Instant deadlineAt) {}
    public record AiTaskTarget(String studentId, String projectId, String assigneeName) {}
}
