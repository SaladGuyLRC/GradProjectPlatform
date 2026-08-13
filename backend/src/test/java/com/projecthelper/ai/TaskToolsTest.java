package com.projecthelper.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projecthelper.ai.tools.TaskTools;
import com.projecthelper.task.Task;
import com.projecthelper.task.TaskService;
import com.projecthelper.task.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskToolsTest {
    @Test
    void findTasksAddsOverdueOnlyToIncompleteTasks() throws Exception {
        TaskService taskService = mock(TaskService.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        when(taskService.listForAi(null, null)).thenReturn(List.of(
                task("overdue", TaskStatus.TODO, Instant.now().minusSeconds(3600)),
                task("upcoming", TaskStatus.IN_PROGRESS, Instant.now().plusSeconds(3600)),
                task("completed", TaskStatus.COMPLETED, Instant.now().minusSeconds(3600))));

        JsonNode result = objectMapper.readTree(new TaskTools(taskService, objectMapper).findTasks(null, null));

        assertTrue(result.get(0).get("overdue").asBoolean());
        assertFalse(result.get(1).get("overdue").asBoolean());
        assertFalse(result.get(2).has("overdue"));
    }

    private Task task(String title, TaskStatus status, Instant deadlineAt) {
        return Task.builder().title(title).status(status).deadlineAt(deadlineAt).build();
    }
}
