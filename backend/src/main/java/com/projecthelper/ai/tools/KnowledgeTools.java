package com.projecthelper.ai.tools;

import com.projecthelper.ai.AiExecutionContext;
import com.projecthelper.knowledge.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KnowledgeTools {
    private final KnowledgeService knowledgeService;

    @Tool(description = "Search the public graduation project knowledge base for regulations, proposal requirements, thesis formatting and defense procedures.")
    public String searchPublicKnowledge(@ToolParam(description = "The user's knowledge-base question") String question) {
        var hits = knowledgeService.search(question);
        if (hits.isEmpty()) return "No relevant content was found in the public knowledge base.";
        StringBuilder result = new StringBuilder("Public knowledge-base results:\n\n");
        for (int i = 0; i < hits.size(); i++) {
            var hit = hits.get(i);
            AiExecutionContext.addCitation(new AiExecutionContext.Citation(hit.documentId(), hit.title(), hit.chunkIndex()));
            result.append("【来源").append(i + 1).append("：").append(hit.title()).append("】\n")
                    .append(hit.content()).append("\n\n");
        }
        return result.toString();
    }
}
