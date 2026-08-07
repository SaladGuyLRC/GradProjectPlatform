package com.projecthelper.ai;

import java.util.List;

public record AiChatResponse(String conversationId, String answer,
                             List<AiExecutionContext.Citation> citations,
                             List<AiExecutionContext.Action> actions) {}
