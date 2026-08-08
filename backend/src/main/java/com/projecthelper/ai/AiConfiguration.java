package com.projecthelper.ai;

import com.projecthelper.ai.tools.AcademicTools;
import com.projecthelper.ai.tools.KnowledgeTools;
import com.projecthelper.ai.tools.TaskTools;
import com.projecthelper.ai.tools.TimeTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfiguration {
    @Bean
    @ConditionalOnBean(ChatModel.class)
    ChatClient chatClient(ChatModel chatModel, AcademicTools academicTools, KnowledgeTools knowledgeTools,
                          TaskTools taskTools, TimeTools timeTools) {
        return ChatClient.builder(chatModel)
                .defaultTools(academicTools, knowledgeTools, taskTools, timeTools)
                .build();
    }
}
