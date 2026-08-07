package com.projecthelper.ai;

import com.projecthelper.ai.tools.KnowledgeTools;
import com.projecthelper.knowledge.KnowledgeService;
import com.projecthelper.knowledge.RedisVectorStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeToolsTest {
    @Mock KnowledgeService knowledgeService;

    @AfterEach void clearContext() { AiExecutionContext.clear(); }

    @Test
    void searchAddsDocumentCitationsToExecutionContext() {
        when(knowledgeService.search("format")).thenReturn(List.of(
                new RedisVectorStore.SearchHit("doc-1", "Thesis Guide", "Use the approved format.", 2)));
        String result = new KnowledgeTools(knowledgeService).searchPublicKnowledge("format");

        assertTrue(result.contains("Thesis Guide"));
        assertEquals("doc-1", AiExecutionContext.citations().getFirst().documentId());
        assertEquals(2, AiExecutionContext.citations().getFirst().chunkIndex());
    }
}
