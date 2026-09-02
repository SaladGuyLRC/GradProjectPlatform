package com.projecthelper.ai.taskdraft;

import com.projecthelper.security.CurrentUserService;
import com.projecthelper.task.TaskService;
import com.projecthelper.task.Task;
import com.projecthelper.task.TaskPriority;
import com.projecthelper.task.TaskType;
import com.projecthelper.user.User;
import com.projecthelper.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import java.util.Optional;

class TaskDraftServiceTest {
    private TaskDraftStore store;
    private TaskService taskService;
    private TaskDraftService service;

    @BeforeEach
    void setUp() {
        store = mock(TaskDraftStore.class);
        taskService = mock(TaskService.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.require()).thenReturn(
                User.builder().id("student-1").realName("Chris Chen").role(UserRole.STUDENT).build());
        DeadlineParser parser = new DeadlineParser(
                Clock.fixed(Instant.parse("2026-08-14T10:00:00Z"), ZoneOffset.UTC));
        service = new TaskDraftService(store, parser, taskService, currentUserService);
    }

    @Test
    void preparesDraftWithoutCreatingTask() {
        when(taskService.resolveAiTarget(null)).thenReturn(
                new TaskService.AiTaskTarget("student-1", "project-1", "Chris Chen"));

        TaskDraftResult result = service.prepare(null, "Finish functional testing", null,
                "tomorrow at 11 AM", "CODE", null);

        assertEquals("PENDING_CONFIRMATION", result.status());
        assertNotNull(result.draft());
        assertEquals("MEDIUM", result.draft().priority());
        assertEquals(Instant.parse("2026-08-15T10:00:00Z"), result.draft().deadlineAt());
        verify(store).save(any(TaskDraft.class));
        verify(taskService, never()).createFromDraft(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void invalidExplicitDeadlineReturnsNeedsInputAndDoesNotSaveDraft() {
        when(taskService.resolveAiTarget(null)).thenReturn(
                new TaskService.AiTaskTarget("student-1", "project-1", "Chris Chen"));

        TaskDraftResult result = service.prepare(null, "Finish functional testing", null,
                "some time later", "CODE", "MEDIUM");

        assertEquals("NEEDS_INPUT", result.status());
        assertTrue(result.issues().stream().anyMatch(issue -> "DEADLINE_UNRECOGNIZED".equals(issue.code())
                && issue.message().contains("Supported formats include")
                && issue.message().contains("tomorrow at 11am")
                && issue.message().contains("Europe/London")));
        verify(store, never()).save(any());
    }

    @Test
    void missingDeadlineUsesTwentyFourHourDefault() {
        when(taskService.resolveAiTarget(null)).thenReturn(
                new TaskService.AiTaskTarget("student-1", "project-1", "Chris Chen"));

        TaskDraftResult result = service.prepare(null, "Finish testing", null, null, null, null);

        assertTrue(result.draft().deadlineWasDefaulted());
        assertEquals(Instant.parse("2026-08-15T10:00:00Z"), result.draft().deadlineAt());
    }

    @Test
    void confirmCreatesTaskAndStoresConfirmedState() {
        TaskDraft draft = pendingDraft(Instant.parse("2026-08-14T10:15:00Z"));
        Task task = Task.builder().id("task-1").title(draft.title()).build();
        when(store.find("draft-1")).thenReturn(Optional.of(draft));
        when(store.acquireConfirmationLock(org.mockito.ArgumentMatchers.eq("draft-1"), any())).thenReturn(true);
        when(taskService.createFromDraft("draft-1", "student-1", draft.title(), null,
                TaskType.CODE, TaskPriority.MEDIUM, draft.deadlineAt())).thenReturn(task);

        TaskDraftService.ConfirmationResult result = service.confirm("draft-1");

        assertEquals("CONFIRMED", result.status());
        assertEquals("task-1", result.task().getId());
        verify(taskService).validateAiTarget("student-1");
        verify(store).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.status() == TaskDraftStatus.CONFIRMED && "task-1".equals(saved.taskId())));
        verify(store).releaseConfirmationLock(org.mockito.ArgumentMatchers.eq("draft-1"), any());
    }

    @Test
    void repeatedConfirmationReturnsExistingTask() {
        TaskDraft confirmed = pendingDraft(Instant.parse("2026-08-14T10:15:00Z"))
                .withStatus(TaskDraftStatus.CONFIRMED, "task-1");
        Task task = Task.builder().id("task-1").build();
        when(store.find("draft-1")).thenReturn(Optional.of(confirmed));
        when(taskService.findBySourceDraftId("draft-1")).thenReturn(Optional.of(task));

        TaskDraftService.ConfirmationResult first = service.confirm("draft-1");
        TaskDraftService.ConfirmationResult second = service.confirm("draft-1");

        assertEquals("task-1", first.task().getId());
        assertEquals("task-1", second.task().getId());
        verify(taskService, never()).createFromDraft(any(), any(), any(), any(), any(), any(), any());
        verify(taskService, times(2)).findBySourceDraftId("draft-1");
    }

    private TaskDraft pendingDraft(Instant expiresAt) {
        return new TaskDraft("draft-1", "student-1", "student-1", "student-1", "project-1",
                "Chris Chen", "Finish testing", null, TaskType.CODE, TaskPriority.MEDIUM,
                Instant.parse("2026-08-15T10:00:00Z"), "15 Aug 2026, 11:00", false,
                TaskDraftStatus.PENDING_CONFIRMATION, null, Instant.parse("2026-08-14T10:00:00Z"), expiresAt);
    }
}
