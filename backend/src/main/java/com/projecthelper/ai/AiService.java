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
            + "When a user asks to create a task, call prepareTaskDraft once and pass the user's original deadline wording in deadlineText. "
            + "Never calculate or provide a timestamp, never claim a task was created from a draft, and wait for the user to confirm the structured draft in the interface. "
            + "Never submit or review weekly reports and never delete important business data. Answer in clear English and cite knowledge-base sources using only the returned original filename and page metadata; never invent page numbers.";

    private final ObjectProvider<ChatClient> chatClientProvider;
    private final AiConversationStore conversations;
    private final CurrentUserService currentUserService;

    @Value("${spring.ai.dashscope.api-key:}")
    private String apiKey;

    public AiChatResponse chat(AiChatRequest request) {
        // 对话上下文按“用户 + conversationId”隔离，防止不同用户读取彼此的历史消息。
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
        ChatClient chatClient = chatClientProvider.getIfAvailable();
        if (chatClient == null) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "AI_NOT_CONFIGURED",
                    "AI is not configured. Set DASHSCOPE_API_KEY.");
        }
        AiExecutionContext.reset();
        try {
            // 工具调用由模型决定，但工具内部仍执行权限和字段验证。
            var response = chatClient.prompt().system(SYSTEM).messages(history).user(message).call();
            String answer = response.content() == null ? "Unable to generate an answer right now." : response.content();
            var draftResult = AiExecutionContext.taskDraftResult();
            if (draftResult != null) {
                answer = draftResult.draft() != null
                        ? "Please review the task details before creating it."
                        : draftResult.issues().stream().map(issue -> issue.message())
                        .reduce((first, second) -> first + "\n" + second).orElse(answer);
            }
            conversations.append(actor.getId(), conversationId,
                    new AiConversationStore.ConversationMessage("user", message));
            conversations.append(actor.getId(), conversationId,
                    new AiConversationStore.ConversationMessage("assistant", answer));
            return new AiChatResponse(conversationId, answer, AiExecutionContext.citations(), AiExecutionContext.actions(),
                    draftResult == null ? null : draftResult.draft(),
                    draftResult == null ? List.of() : draftResult.issues());
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
