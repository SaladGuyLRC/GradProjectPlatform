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
            String sourceName = hit.originalFilename() == null || hit.originalFilename().isBlank()
                    ? hit.title() : hit.originalFilename();
            AiExecutionContext.addCitation(new AiExecutionContext.Citation(hit.documentId(), hit.title(),
                    sourceName, hit.chunkIndex(), hit.pageStart(), hit.pageEnd()));
            result.append("[Source ").append(i + 1).append(": ").append(sourceName)
                    .append(", ").append(pageLabel(hit.pageStart(), hit.pageEnd())).append("]\n")
                    .append(hit.content()).append("\n\n");
        }
        return result.toString();
    }

    private String pageLabel(int start, int end) {
        if (start <= 0) return "page unavailable";
        return start == end ? "Page " + start : "Pages " + start + "-" + end;
    }
}
