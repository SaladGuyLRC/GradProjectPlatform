package com.projecthelper.ai.taskdraft;

import com.projecthelper.common.BusinessException;
import com.projecthelper.security.CurrentUserService;
import com.projecthelper.task.Task;
import com.projecthelper.task.TaskPriority;
import com.projecthelper.task.TaskService;
import com.projecthelper.task.TaskType;
import com.projecthelper.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskDraftService {
    private static final Duration DRAFT_LIFETIME = Duration.ofMinutes(15);
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_DESCRIPTION_LENGTH = 4000;

    private final TaskDraftStore store;
    private final DeadlineParser deadlineParser;
    private final TaskService taskService;
    private final CurrentUserService currentUserService;

    public TaskDraftResult prepare(String studentName, String title, String description, String deadlineText,
                                   String type, String priority) {
        User actor = currentUserService.require();
        List<TaskDraftResult.Issue> issues = new ArrayList<>();
        String normalizedTitle = normalizeTitle(title, issues);
        String normalizedDescription = normalizeDescription(description, issues);
        TaskType normalizedType = normalizeType(type);
        TaskPriority normalizedPriority = normalizePriority(priority, issues);

        Instant now = deadlineParser.now();
        boolean deadlineWasDefaulted = deadlineText == null || deadlineText.isBlank();
        Instant deadline = deadlineWasDefaulted ? now.plus(Duration.ofHours(24))
                : parseDeadline(deadlineText, issues);
        if (deadline != null && !deadline.isAfter(now)) {
            issues.add(new TaskDraftResult.Issue("deadline", "DEADLINE_IN_PAST",
                    "The deadline must be in the future."));
        }

        TaskService.AiTaskTarget target = null;
        try {
            target = taskService.resolveAiTarget(studentName);
        } catch (BusinessException exception) {
            issues.add(issueFor(exception));
            if (exception.getStatus() == HttpStatus.FORBIDDEN) return TaskDraftResult.rejected(issueFor(exception));
        }
        if (!issues.isEmpty()) return TaskDraftResult.needsInput(issues);

        Instant createdAt = deadlineParser.now();
        TaskDraft draft = new TaskDraft(UUID.randomUUID().toString(), actor.getId(), actor.getId(),
                target.studentId(), target.projectId(), target.assigneeName(), normalizedTitle,
                normalizedDescription, normalizedType, normalizedPriority, deadline,
                deadlineParser.display(deadline), deadlineWasDefaulted, TaskDraftStatus.PENDING_CONFIRMATION,
                null, createdAt, createdAt.plus(DRAFT_LIFETIME));
        store.save(draft);
        return TaskDraftResult.pending(draft);
    }

    public ConfirmationResult confirm(String draftId) {
        User actor = currentUserService.require();
        TaskDraft initial = requireOwnedDraft(draftId, actor.getId());
        ConfirmationResult terminal = confirmedResult(initial);
        if (terminal != null) return terminal;
        validatePending(initial);

        String lockToken = UUID.randomUUID().toString();
        if (!store.acquireConfirmationLock(draftId, lockToken)) {
            TaskDraft latest = requireOwnedDraft(draftId, actor.getId());
            terminal = confirmedResult(latest);
            if (terminal != null) return terminal;
            throw new BusinessException(HttpStatus.CONFLICT, "DRAFT_CONFIRMATION_IN_PROGRESS",
                    "This task draft is already being confirmed. Please try again.");
        }

        try {
            TaskDraft draft = requireOwnedDraft(draftId, actor.getId());
            terminal = confirmedResult(draft);
            if (terminal != null) return terminal;
            validatePending(draft);
            if (!draft.deadlineAt().isAfter(deadlineParser.now())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "DEADLINE_IN_PAST",
                        "The task was not created because the deadline is now in the past.");
            }
            taskService.validateAiTarget(draft.studentId());
            Task task = taskService.createFromDraft(draft.draftId(), draft.studentId(), draft.title(),
                    draft.description(), draft.type(), draft.priority(), draft.deadlineAt());
            try {
                store.save(draft.withStatus(TaskDraftStatus.CONFIRMED, task.getId()));
            } catch (RuntimeException exception) {
                log.warn("Task {} was created for draft {}, but the confirmed draft state could not be saved",
                        task.getId(), draftId, exception);
            }
            return new ConfirmationResult("CONFIRMED", task);
        } finally {
            store.releaseConfirmationLock(draftId, lockToken);
        }
    }

    public CancellationResult cancel(String draftId) {
        User actor = currentUserService.require();
        TaskDraft draft = requireOwnedDraft(draftId, actor.getId());
        if (draft.status() == TaskDraftStatus.CANCELLED) return new CancellationResult("CANCELLED", draftId);
        if (draft.status() == TaskDraftStatus.CONFIRMED) {
            throw new BusinessException(HttpStatus.CONFLICT, "DRAFT_ALREADY_CONFIRMED",
                    "This task draft has already been confirmed and cannot be cancelled.");
        }
        validateNotExpired(draft);
        String lockToken = UUID.randomUUID().toString();
        if (!store.acquireConfirmationLock(draftId, lockToken)) {
            throw new BusinessException(HttpStatus.CONFLICT, "DRAFT_CONFIRMATION_IN_PROGRESS",
                    "This task draft is currently being confirmed and cannot be cancelled.");
        }
        try {
            draft = requireOwnedDraft(draftId, actor.getId());
            if (draft.status() == TaskDraftStatus.CANCELLED) return new CancellationResult("CANCELLED", draftId);
            if (draft.status() == TaskDraftStatus.CONFIRMED) {
                throw new BusinessException(HttpStatus.CONFLICT, "DRAFT_ALREADY_CONFIRMED",
                        "This task draft has already been confirmed and cannot be cancelled.");
            }
            validateNotExpired(draft);
            store.save(draft.withStatus(TaskDraftStatus.CANCELLED, null));
            return new CancellationResult("CANCELLED", draftId);
        } finally {
            store.releaseConfirmationLock(draftId, lockToken);
        }
    }

    private ConfirmationResult confirmedResult(TaskDraft draft) {
        if (draft.status() != TaskDraftStatus.CONFIRMED) return null;
        Task task = taskService.findBySourceDraftId(draft.draftId())
                .orElseThrow(() -> new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "TASK_STATE_UNAVAILABLE",
                        "The confirmed task could not be loaded. Please try again."));
        return new ConfirmationResult("CONFIRMED", task);
    }

    private void validatePending(TaskDraft draft) {
        if (draft.status() == TaskDraftStatus.CANCELLED) {
            throw new BusinessException(HttpStatus.CONFLICT, "DRAFT_CANCELLED",
                    "This task draft was cancelled. No task was created.");
        }
        validateNotExpired(draft);
    }

    private void validateNotExpired(TaskDraft draft) {
        if (draft.status() == TaskDraftStatus.EXPIRED || !draft.expiresAt().isAfter(deadlineParser.now())) {
            if (draft.status() != TaskDraftStatus.EXPIRED) {
                try { store.save(draft.withStatus(TaskDraftStatus.EXPIRED, null)); }
                catch (RuntimeException exception) { log.warn("Could not mark task draft {} as expired", draft.draftId(), exception); }
            }
            throw new BusinessException(HttpStatus.CONFLICT, "DRAFT_EXPIRED",
                    "This task draft has expired. No task was created.");
        }
    }

    private TaskDraft requireOwnedDraft(String draftId, String actorId) {
        TaskDraft draft = store.find(draftId).orElseThrow(() ->
                new BusinessException(HttpStatus.NOT_FOUND, "DRAFT_NOT_FOUND", "Task draft not found."));
        if (!actorId.equals(draft.ownerUserId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "You do not have permission to access this task draft.");
        }
        return draft;
    }

    private Instant parseDeadline(String deadlineText, List<TaskDraftResult.Issue> issues) {
        DeadlineParser.ParseResult result = deadlineParser.parse(deadlineText);
        if (result.status() == DeadlineParser.ParseStatus.AMBIGUOUS) {
            issues.add(new TaskDraftResult.Issue("deadline", "DEADLINE_AMBIGUOUS",
                    "The deadline is ambiguous. Please provide a specific date and time. "
                            + deadlineParser.supportedExamplesMessage()));
            return null;
        }
        if (result.status() == DeadlineParser.ParseStatus.UNRECOGNIZED) {
            issues.add(new TaskDraftResult.Issue("deadline", "DEADLINE_UNRECOGNIZED",
                    "I could not understand the deadline. Please provide a clear date and time. "
                            + deadlineParser.supportedExamplesMessage()));
            return null;
        }
        return result.deadlineAt();
    }

    private String normalizeTitle(String title, List<TaskDraftResult.Issue> issues) {
        if (title == null || title.isBlank()) {
            issues.add(new TaskDraftResult.Issue("title", "TITLE_REQUIRED", "Please provide a title for the task."));
            return null;
        }
        String value = title.trim();
        if (value.length() > MAX_TITLE_LENGTH) {
            issues.add(new TaskDraftResult.Issue("title", "TITLE_TOO_LONG",
                    "The task title must be 200 characters or fewer."));
        }
        return value;
    }

    private String normalizeDescription(String description, List<TaskDraftResult.Issue> issues) {
        if (description == null || description.isBlank()) return null;
        String value = description.trim();
        if (value.length() > MAX_DESCRIPTION_LENGTH) {
            issues.add(new TaskDraftResult.Issue("description", "DESCRIPTION_TOO_LONG",
                    "The task description must be 4000 characters or fewer."));
        }
        return value;
    }

    private TaskType normalizeType(String type) {
        if (type == null || type.isBlank()) return TaskType.OTHER;
        try { return TaskType.valueOf(type.trim().toUpperCase(Locale.UK)); }
        catch (IllegalArgumentException ignored) { return TaskType.OTHER; }
    }

    private TaskPriority normalizePriority(String priority, List<TaskDraftResult.Issue> issues) {
        if (priority == null || priority.isBlank()) return TaskPriority.MEDIUM;
        try { return TaskPriority.valueOf(priority.trim().toUpperCase(Locale.UK)); }
        catch (IllegalArgumentException exception) {
            issues.add(new TaskDraftResult.Issue("priority", "PRIORITY_INVALID",
                    "Priority must be Low, Medium or High."));
            return TaskPriority.MEDIUM;
        }
    }

    private TaskDraftResult.Issue issueFor(BusinessException exception) {
        String field = exception.getCode().startsWith("ASSIGNEE") ? "student"
                : "PROJECT_REQUIRED".equals(exception.getCode()) ? "project" : null;
        return new TaskDraftResult.Issue(field, exception.getCode(), exception.getMessage());
    }

    public record ConfirmationResult(String status, Task task) {}
    public record CancellationResult(String status, String draftId) {}
}
