package com.projecthelper.knowledge;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface KnowledgeDocumentRepository extends MongoRepository<KnowledgeDocument, String> {
    Optional<KnowledgeDocument> findBySha256(String sha256);
    List<KnowledgeDocument> findAllByOrderByCreatedAtDesc();
    List<KnowledgeDocument> findByStatusAndUpdatedAtBefore(KnowledgeStatus status, Instant before);
}
