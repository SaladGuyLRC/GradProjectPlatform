package com.projecthelper.knowledge;

import com.projecthelper.knowledge.parser.DocumentParser;
import com.projecthelper.knowledge.parser.DocumentParserRegistry;
import com.projecthelper.knowledge.parser.ParsedDocument;
import com.projecthelper.knowledge.parser.ParsedPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeIndexingServiceTest {
    @Mock KnowledgeDocumentRepository documentRepository;
    @Mock DocumentParserRegistry parserRegistry;
    @Mock RedisVectorStore vectorStore;
    @Mock DocumentParser parser;

    @Test
    void indexesChunksWithOriginalFilenameAndPhysicalPages() throws Exception {
        KnowledgeDocument document = KnowledgeDocument.builder()
                .id("doc-1")
                .title("Thesis Guide")
                .originalFilename("thesis-guide.pdf")
                .storedPath("/tmp/thesis-guide.pdf")
                .mimeType("application/pdf")
                .build();
        KnowledgeProperties properties = new KnowledgeProperties();
        properties.setChunkSize(100);
        properties.setChunkOverlap(10);
        when(documentRepository.findById("doc-1")).thenReturn(Optional.of(document));
        when(parserRegistry.requireParser("thesis-guide.pdf", "application/pdf")).thenReturn(parser);
        when(parser.parse(any(Path.class))).thenReturn(new ParsedDocument(List.of(
                new ParsedPage(2, "Page two content."),
                new ParsedPage(5, "Page five content.")), 5));
        when(vectorStore.replaceDocument(eq("doc-1"), eq("Thesis Guide"),
                eq("thesis-guide.pdf"), any())).thenReturn(2);
        KnowledgeIndexingService service = new KnowledgeIndexingService(
                documentRepository, parserRegistry, vectorStore, properties);

        service.indexAsync("doc-1");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<KnowledgeChunk>> chunks = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).replaceDocument(eq("doc-1"), eq("Thesis Guide"),
                eq("thesis-guide.pdf"), chunks.capture());
        assertEquals(List.of(
                new KnowledgeChunk("Page two content.", 0, 2, 2),
                new KnowledgeChunk("Page five content.", 1, 5, 5)), chunks.getValue());
        assertEquals(KnowledgeStatus.INDEXED, document.getStatus());
        assertEquals(2, document.getChunkCount());
    }
}
