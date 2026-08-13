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
import static org.mockito.Mockito.when;

class AiServiceConfigurationTest {
    @Test
    void chatClientFactoryDoesNotDependOnBeanRegistrationOrder() throws NoSuchMethodException {
        var method = AiConfiguration.class.getDeclaredMethod("chatClient", org.springframework.ai.chat.model.ChatModel.class,
                com.projecthelper.ai.tools.AcademicTools.class, com.projecthelper.ai.tools.KnowledgeTools.class,
                com.projecthelper.ai.tools.TaskTools.class, com.projecthelper.ai.tools.TimeTools.class);

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
}
