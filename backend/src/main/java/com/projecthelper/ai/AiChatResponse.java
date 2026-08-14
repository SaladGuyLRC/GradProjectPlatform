package com.projecthelper.ai;

import com.projecthelper.ai.taskdraft.TaskDraftResult;

import java.util.List;

public record AiChatResponse(String conversationId, String answer,
                             List<AiExecutionContext.Citation> citations,
                             List<AiExecutionContext.Action> actions,
                             TaskDraftResult.TaskDraftView taskDraft,
                             List<TaskDraftResult.Issue> issues) {}
