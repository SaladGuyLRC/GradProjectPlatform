package com.projecthelper.knowledge;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@Document("knowledge_documents")
public class KnowledgeDocument {
    @Id private String id;
    private String title;
    private String description;
    private String originalFilename;
    private String storedFilename;
    private String storedPath;
    private String mimeType;
    private long fileSize;
    @Indexed(unique = true) private String sha256;
    private String uploaderId;
    @Indexed private KnowledgeStatus status;
    private int chunkCount;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant indexedAt;
}
