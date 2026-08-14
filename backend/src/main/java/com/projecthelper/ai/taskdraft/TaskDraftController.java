package com.projecthelper.ai.taskdraft;

import com.projecthelper.common.ApiResponse;
import com.projecthelper.common.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api/ai/task-drafts")
@PreAuthorize("hasAnyRole('STUDENT','MENTOR')")
@RequiredArgsConstructor
public class TaskDraftController {
    private final TaskDraftService taskDraftService;

    @PostMapping("/{draftId}/confirm")
    public ApiResponse<TaskDraftService.ConfirmationResult> confirm(@PathVariable String draftId) {
        try {
            return ApiResponse.success(taskDraftService.confirm(draftId));
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("Task draft confirmation failed for draft {}", draftId, exception);
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "TASK_CONFIRMATION_UNAVAILABLE",
                    "The task creation result could not be confirmed. Please retry; retrying will not create a duplicate.");
        }
    }

    @PostMapping("/{draftId}/cancel")
    public ApiResponse<TaskDraftService.CancellationResult> cancel(@PathVariable String draftId) {
        try {
            return ApiResponse.success(taskDraftService.cancel(draftId));
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("Task draft cancellation failed for draft {}", draftId, exception);
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "TASK_CANCELLATION_UNAVAILABLE",
                    "The task draft could not be cancelled because the service is temporarily unavailable.");
        }
    }
}
