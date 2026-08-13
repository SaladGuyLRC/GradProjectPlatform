package com.projecthelper.ai;

import com.projecthelper.common.BusinessException;
import com.projecthelper.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiService {
    private static final String SYSTEM = "You are the ProjectHelper graduation project assistant. Use tools only for data the authenticated user may access. "
            + "A student may query only their own project, reports and tasks; a mentor may query only assigned students; an administrator cannot read student business data. "
            + "For task results, treat the provided overdue field as authoritative: label every task with overdue=true as Overdue, and never label overdue=false or an absent field as Overdue. "
            + "Never submit or review weekly reports and never delete important business data. Answer in clear English and cite knowledge-base sources.";

    private final ObjectProvider<ChatClient> chatClientProvider;
    private final AiConversationStore conversations;
    private final CurrentUserService currentUserService;

    @Value("${spring.ai.dashscope.api-key:}")
    private String apiKey;

    public AiChatResponse chat(AiChatRequest request) {
        var actor = currentUserService.require();
        if (apiKey == null || apiKey.isBlank() || "replace-me".equals(apiKey)) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "AI_NOT_CONFIGURED",
                    "AI is not configured. Set DASHSCOPE_API_KEY.");
        }
        String message = request.message().trim();
        String conversationId = request.conversationId() == null || request.conversationId().isBlank()
                ? UUID.randomUUID().toString() : request.conversationId().trim();
        if (conversationId.length() > 100) throw BusinessException.badRequest("conversationId is too long");
        List<Message> history = new ArrayList<>();
        for (var saved : conversations.load(actor.getId(), conversationId)) {
            history.add("assistant".equals(saved.role()) ? new AssistantMessage(saved.content()) : new UserMessage(saved.content()));
        }
        conversations.append(actor.getId(), conversationId, new AiConversationStore.ConversationMessage("user", message));
        ChatClient chatClient = chatClientProvider.getIfAvailable();
        if (chatClient == null) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "AI_NOT_CONFIGURED",
                    "AI is not configured. Set DASHSCOPE_API_KEY.");
        }
        AiExecutionContext.reset();
        try {
            var response = chatClient.prompt().system(SYSTEM).messages(history).user(message).call();
            String answer = response.content() == null ? "Unable to generate an answer right now." : response.content();
            conversations.append(actor.getId(), conversationId,
                    new AiConversationStore.ConversationMessage("assistant", answer));
            return new AiChatResponse(conversationId, answer, AiExecutionContext.citations(), AiExecutionContext.actions());
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE",
                    "AI is temporarily unavailable. Check DASHSCOPE_API_KEY.");
        } finally {
            AiExecutionContext.clear();
        }
    }
}
