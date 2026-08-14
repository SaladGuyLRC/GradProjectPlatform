package com.projecthelper.ai.taskdraft;

import java.time.Instant;
import java.util.List;

public record TaskDraftResult(String status, TaskDraftView draft, List<Issue> issues) {
    public static TaskDraftResult pending(TaskDraft draft) {
        return new TaskDraftResult("PENDING_CONFIRMATION", TaskDraftView.from(draft), List.of());
    }

    public static TaskDraftResult needsInput(List<Issue> issues) {
        return new TaskDraftResult("NEEDS_INPUT", null, List.copyOf(issues));
    }

    public static TaskDraftResult rejected(Issue issue) {
        return new TaskDraftResult("REJECTED", null, List.of(issue));
    }

    public static TaskDraftResult failed() {
        return new TaskDraftResult("FAILED", null, List.of(new Issue(null, "SERVICE_UNAVAILABLE",
                "The task draft could not be prepared because the service is temporarily unavailable. No task was created.")));
    }

    public record Issue(String field, String code, String message) {}

    public record TaskDraftView(String draftId, TaskDraftStatus status, String title, String description,
                                String assigneeName, String type, String priority, Instant deadlineAt,
                                String deadlineDisplay, boolean deadlineWasDefaulted, Instant expiresAt,
                                String taskId) {
        public static TaskDraftView from(TaskDraft draft) {
            return new TaskDraftView(draft.draftId(), draft.status(), draft.title(), draft.description(),
                    draft.assigneeName(), draft.type().name(), draft.priority().name(), draft.deadlineAt(),
                    draft.deadlineDisplay(), draft.deadlineWasDefaulted(), draft.expiresAt(), draft.taskId());
        }
    }
}
