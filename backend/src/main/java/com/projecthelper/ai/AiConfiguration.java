package com.projecthelper.ai;

import com.projecthelper.ai.tools.AcademicTools;
import com.projecthelper.ai.tools.KnowledgeTools;
import com.projecthelper.ai.tools.TaskTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfiguration {
    @Bean
    ChatClient chatClient(ChatModel chatModel, AcademicTools academicTools, KnowledgeTools knowledgeTools,
                          TaskTools taskTools) {
        return ChatClient.builder(chatModel)
                .defaultTools(academicTools, knowledgeTools, taskTools)
                .build();
    }
}
