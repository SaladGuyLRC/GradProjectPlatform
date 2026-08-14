package com.projecthelper.ai;

import com.projecthelper.common.BusinessException;
import com.projecthelper.security.CurrentUserService;
import com.projecthelper.user.User;
import com.projecthelper.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiServiceConfigurationTest {
    @Test
    void chatClientFactoryDoesNotDependOnBeanRegistrationOrder() throws NoSuchMethodException {
        var method = AiConfiguration.class.getDeclaredMethod("chatClient", org.springframework.ai.chat.model.ChatModel.class,
                com.projecthelper.ai.tools.AcademicTools.class, com.projecthelper.ai.tools.KnowledgeTools.class,
                com.projecthelper.ai.tools.TaskTools.class);

        assertNull(method.getAnnotation(ConditionalOnBean.class));
    }

    @Test
    void missingDashScopeKeyReturnsConfigurationError() {
        ObjectProvider<ChatClient> provider = mock(ObjectProvider.class);
        AiConversationStore conversations = mock(AiConversationStore.class);
        CurrentUserService currentUser = mock(CurrentUserService.class);
        when(currentUser.require()).thenReturn(User.builder().id("s1").role(UserRole.STUDENT).build());
        AiService service = new AiService(provider, conversations, currentUser);
        ReflectionTestUtils.setField(service, "apiKey", "replace-me");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.chat(new AiChatRequest("What are my tasks?", "test")));
        assertEquals("AI_NOT_CONFIGURED", exception.getCode());
        assertEquals(503, exception.getStatus().value());
    }

    @Test
    void failedChatDoesNotSaveTheUserMessage() {
        ObjectProvider<ChatClient> provider = mock(ObjectProvider.class);
        ChatClient chatClient = mock(ChatClient.class);
        AiConversationStore conversations = mock(AiConversationStore.class);
        CurrentUserService currentUser = mock(CurrentUserService.class);
        when(currentUser.require()).thenReturn(User.builder().id("s1").role(UserRole.STUDENT).build());
        when(provider.getIfAvailable()).thenReturn(chatClient);
        when(chatClient.prompt()).thenThrow(new IllegalStateException("AI request failed"));
        AiService service = new AiService(provider, conversations, currentUser);
        ReflectionTestUtils.setField(service, "apiKey", "configured-key");

        assertThrows(BusinessException.class,
                () -> service.chat(new AiChatRequest("What are the requirements?", "test")));

        verify(conversations, never()).append(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void successfulChatSavesUserThenAssistantMessages() {
        ObjectProvider<ChatClient> provider = mock(ObjectProvider.class);
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec prompt = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec response = mock(ChatClient.CallResponseSpec.class);
        AiConversationStore conversations = mock(AiConversationStore.class);
        CurrentUserService currentUser = mock(CurrentUserService.class);
        when(currentUser.require()).thenReturn(User.builder().id("s1").role(UserRole.STUDENT).build());
        when(provider.getIfAvailable()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(prompt);
        when(prompt.system(org.mockito.ArgumentMatchers.anyString())).thenReturn(prompt);
        when(prompt.messages(org.mockito.ArgumentMatchers.<org.springframework.ai.chat.messages.Message>anyList())).thenReturn(prompt);
        when(prompt.user("Hello")).thenReturn(prompt);
        when(prompt.call()).thenReturn(response);
        when(response.content()).thenReturn("Hi");
        AiService service = new AiService(provider, conversations, currentUser);
        ReflectionTestUtils.setField(service, "apiKey", "configured-key");

        service.chat(new AiChatRequest("Hello", "test"));

        var order = inOrder(conversations);
        order.verify(conversations).append("s1", "test", new AiConversationStore.ConversationMessage("user", "Hello"));
        order.verify(conversations).append("s1", "test", new AiConversationStore.ConversationMessage("assistant", "Hi"));
    }
}
