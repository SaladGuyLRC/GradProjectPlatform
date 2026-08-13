package com.projecthelper.ai.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.projecthelper.ai.AiExecutionContext;
import com.projecthelper.task.*;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TaskTools {
    private final TaskService taskService;
    private final ObjectMapper objectMapper;

    @Tool(description = "Find tasks. A student can query their own tasks; a mentor can query tasks for an assigned student. "
            + "When summarising results, mark every task with overdue=true as Overdue, do not mark overdue=false, and do not infer overdue when the field is absent.")
    public String findTasks(
            @ToolParam(description = "Assigned student name for a mentor; omit for a student", required = false) String studentName,
            @ToolParam(description = "Optional status: TODO, IN_PROGRESS or COMPLETED", required = false) String status) {
        TaskStatus parsed = status == null || status.isBlank() ? null : TaskStatus.valueOf(status.toUpperCase());
        Instant now = Instant.now();
        List<ObjectNode> tasks = taskService.listForAi(studentName, parsed).stream().map(task -> {
            ObjectNode result = objectMapper.valueToTree(task);
            if (task.getStatus() != TaskStatus.COMPLETED) {
                result.put("overdue", task.getDeadlineAt() != null && task.getDeadlineAt().isBefore(now));
            }
            return result;
        }).toList();
        try { return objectMapper.writeValueAsString(tasks); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Failed to serialize tasks", exception); }
    }

    @Tool(description = "Create a graduation project task only when the user explicitly asks. Do not call without a target student, title and deadline.")
    public String createTask(
            @ToolParam(description = "Assigned student name for a mentor; omit for a student", required = false) String studentName,
            @ToolParam(description = "Task title") String title,
            @ToolParam(description = "Task description", required = false) String description,
            @ToolParam(description = "MEETING, PROGRESS, DOCUMENT, CODE, EXPERIMENT or OTHER") String type,
            @ToolParam(description = "LOW, MEDIUM or HIGH", required = false) String priority,
            @ToolParam(description = "Deadline as a Unix seconds timestamp") Long deadlineAt) {
        Task task = taskService.createFromAi(studentName, title, description,
                TaskType.valueOf(type.toUpperCase()),
                priority == null || priority.isBlank() ? TaskPriority.MEDIUM : TaskPriority.valueOf(priority.toUpperCase()),
                Instant.ofEpochSecond(deadlineAt));
        AiExecutionContext.addAction(new AiExecutionContext.Action("TASK_CREATED", task.getId(), task.getTitle()));
        return "Task created successfully. ID=" + task.getId() + ", title=" + task.getTitle() + ", deadline=" + task.getDeadlineAt();
    }
}
