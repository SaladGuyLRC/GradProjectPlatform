package com.projecthelper.ai.taskdraft;

import com.projecthelper.task.TaskPriority;
import com.projecthelper.task.TaskType;

import java.time.Instant;

public record TaskDraft(
        String draftId,
        String ownerUserId,
        String creatorId,
        String studentId,
        String projectId,
        String assigneeName,
        String title,
        String description,
        TaskType type,
        TaskPriority priority,
        Instant deadlineAt,
        String deadlineDisplay,
        boolean deadlineWasDefaulted,
        TaskDraftStatus status,
        String taskId,
        Instant createdAt,
        Instant expiresAt
) {
    public TaskDraft withStatus(TaskDraftStatus newStatus, String newTaskId) {
        return new TaskDraft(draftId, ownerUserId, creatorId, studentId, projectId, assigneeName, title,
                description, type, priority, deadlineAt, deadlineDisplay, deadlineWasDefaulted,
                newStatus, newTaskId, createdAt, expiresAt);
    }
}
