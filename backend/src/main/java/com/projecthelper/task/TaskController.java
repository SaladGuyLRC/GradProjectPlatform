package com.projecthelper.task;

import com.projecthelper.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/tasks")
@PreAuthorize("hasAnyRole('STUDENT','MENTOR')")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;

    @GetMapping
    public ApiResponse<Page<Task>> list(@RequestParam(required = false) String studentId,
                                        @RequestParam(required = false) TaskStatus status,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(taskService.list(studentId, status, page, size));
    }

    @PostMapping
    public ApiResponse<Task> create(@Valid @RequestBody TaskRequest request) {
        return ApiResponse.success(taskService.create(request.command()));
    }

    @PutMapping("/{id}")
    public ApiResponse<Task> update(@PathVariable String id, @Valid @RequestBody TaskRequest request) {
        return ApiResponse.success(taskService.update(id, request.command()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        taskService.delete(id);
        return ApiResponse.success(null);
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Task> status(@PathVariable String id, @Valid @RequestBody StatusRequest request) {
        return ApiResponse.success(taskService.changeStatus(id, request.status()));
    }

    public record TaskRequest(String studentId, @NotBlank String title, String description,
                              @NotNull TaskType type, @NotNull TaskPriority priority,
                              @NotNull @Future Instant deadlineAt) {
        TaskService.TaskCommand command() {
            return new TaskService.TaskCommand(studentId, title, description, type, priority, deadlineAt);
        }
    }
    public record StatusRequest(@NotNull TaskStatus status) {}
}
