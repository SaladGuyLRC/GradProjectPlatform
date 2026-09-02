package com.projecthelper.knowledge;

import com.projecthelper.knowledge.parser.DocumentParserRegistry;
import com.projecthelper.knowledge.parser.ParsedPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeIndexingService {
    private final KnowledgeDocumentRepository documentRepository;
    private final DocumentParserRegistry parserRegistry;
    private final RedisVectorStore vectorStore;
    private final KnowledgeProperties properties;

    @Async
    public void indexAsync(String documentId) {
        KnowledgeDocument document = documentRepository.findById(documentId).orElse(null);
        if (document == null) return;
        try {
            document.setStatus(KnowledgeStatus.PROCESSING);
            document.setFailureReason(null);
            document.setUpdatedAt(Instant.now());
            documentRepository.save(document);
            var parser = parserRegistry.requireParser(document.getOriginalFilename(), document.getMimeType());
            var parsed = parser.parse(Path.of(document.getStoredPath()));
            List<KnowledgeChunk> chunks = split(parsed.pages());
            int count = vectorStore.replaceDocument(document.getId(), document.getTitle(),
                    document.getOriginalFilename(), chunks);
            document.setStatus(KnowledgeStatus.INDEXED);
            document.setChunkCount(count);
            document.setIndexedAt(Instant.now());
            document.setUpdatedAt(Instant.now());
            documentRepository.save(document);
        } catch (Exception exception) {
            log.error("Knowledge indexing failed: {}", documentId, exception);
            document.setStatus(KnowledgeStatus.FAILED);
            document.setFailureReason(trim(exception.getMessage()));
            document.setUpdatedAt(Instant.now());
            documentRepository.save(document);
        }
    }

    private List<KnowledgeChunk> split(List<ParsedPage> pages) {
        int chunkSize = properties.getChunkSize();
        int overlap = Math.min(properties.getChunkOverlap(), chunkSize - 1);
        List<KnowledgeChunk> chunks = new ArrayList<>();
        int chunkIndex = 0;
        for (ParsedPage page : pages) {
            String normalized = page.text().replaceAll("[\\t ]+", " ")
                    .replaceAll("\\n{3,}", "\\n\\n").trim();
            int start = 0;
            while (start < normalized.length()) {
                int end = Math.min(normalized.length(), start + chunkSize);
                if (end < normalized.length()) {
                    int boundary = normalized.lastIndexOf('\n', end);
                    if (boundary > start + chunkSize / 2) end = boundary;
                }
                String chunk = normalized.substring(start, end).trim();
                if (!chunk.isBlank()) {
                    chunks.add(new KnowledgeChunk(chunk, chunkIndex++, page.pageNumber(), page.pageNumber()));
                }
                if (end == normalized.length()) break;
                start = Math.max(start + 1, end - overlap);
            }
        }
        return chunks;
    }

    private String trim(String message) {
        if (message == null) return "Unknown error";
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
