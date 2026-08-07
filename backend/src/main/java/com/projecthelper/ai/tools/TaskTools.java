package com.projecthelper.ai.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projecthelper.ai.AiExecutionContext;
import com.projecthelper.task.*;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class TaskTools {
    private final TaskService taskService;
    private final ObjectMapper objectMapper;

    @Tool(description = "Find tasks. A student can query their own tasks; a mentor can query tasks for an assigned student.")
    public String findTasks(
            @ToolParam(description = "Assigned student name for a mentor; omit for a student", required = false) String studentName,
            @ToolParam(description = "Optional status: TODO, IN_PROGRESS or COMPLETED", required = false) String status) {
        TaskStatus parsed = status == null || status.isBlank() ? null : TaskStatus.valueOf(status.toUpperCase());
        try { return objectMapper.writeValueAsString(taskService.listForAi(studentName, parsed)); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("序列化任务失败", exception); }
    }

    @Tool(description = "Create a graduation project task only when the user explicitly asks. Do not call without a target student, title and deadline.")
    public String createTask(
            @ToolParam(description = "Assigned student name for a mentor; omit for a student") String studentName,
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
        return "待办创建成功，任务ID=" + task.getId() + "，标题=" + task.getTitle() + "，截止时间=" + task.getDeadlineAt();
    }
}
