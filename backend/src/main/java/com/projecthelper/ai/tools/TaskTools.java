package com.projecthelper.ai.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.projecthelper.ai.AiExecutionContext;
import com.projecthelper.ai.taskdraft.TaskDraftResult;
import com.projecthelper.ai.taskdraft.TaskDraftService;
import com.projecthelper.task.TaskService;
import com.projecthelper.task.TaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskTools {
    private final TaskService taskService;
    private final ObjectMapper objectMapper;
    private final TaskDraftService taskDraftService;

    @Tool(description = "Find tasks. A student can query their own tasks; a mentor can query tasks for an assigned student. "
            + "When summarising results, mark every task with overdue=true as Overdue, do not mark overdue=false, and do not infer overdue when the field is absent.")
    public String findTasks(
            @ToolParam(description = "Assigned student name for a mentor; omit for a student", required = false) String studentName,
            @ToolParam(description = "Optional status: TODO, IN_PROGRESS or COMPLETED", required = false) String status) {
        // 工具返回结构化 JSON；overdue 由后端根据当前时间计算，模型只能读取不能推断。
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

    @Tool(description = "Prepare a task draft when the user asks to create a task. This tool never creates the task. "
            + "Pass the user's original deadline wording in deadlineText. Do not calculate timestamps. "
            + "The task is created only after the user confirms the draft in the interface.")
    public String prepareTaskDraft(
            @ToolParam(description = "Assigned student name for a mentor; omit for a student", required = false) String studentName,
            @ToolParam(description = "Task title") String title,
            @ToolParam(description = "Task description", required = false) String description,
            @ToolParam(description = "The user's original deadline wording, for example tomorrow at 11 AM; omit only when the user gave no deadline", required = false) String deadlineText,
            @ToolParam(description = "MEETING, PROGRESS, DOCUMENT, CODE, EXPERIMENT or OTHER", required = false) String type,
            @ToolParam(description = "LOW, MEDIUM or HIGH", required = false) String priority) {
        // AI 只能请求“准备草稿”，真正创建必须等待用户在界面点击 Confirm。
        TaskDraftResult result;
        try {
            result = taskDraftService.prepare(studentName, title, description, deadlineText, type, priority);
        } catch (RuntimeException exception) {
            log.error("Failed to prepare an AI task draft", exception);
            result = TaskDraftResult.failed();
        }
        AiExecutionContext.setTaskDraftResult(result);
        try { return objectMapper.writeValueAsString(result); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Failed to serialize task draft", exception); }
    }
}
