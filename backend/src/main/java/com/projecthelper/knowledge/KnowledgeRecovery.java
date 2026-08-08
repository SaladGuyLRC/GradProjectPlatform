package com.projecthelper.knowledge;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class KnowledgeRecovery implements ApplicationRunner {
    private final KnowledgeDocumentRepository repository;

    @Override
    public void run(ApplicationArguments args) {
        repository.findByStatusAndUpdatedAtBefore(KnowledgeStatus.PROCESSING, Instant.now().minus(10, ChronoUnit.MINUTES))
                .forEach(document -> {
                    document.setStatus(KnowledgeStatus.FAILED);
                    document.setFailureReason("The application restarted or indexing timed out. Reindex the document.");
                    document.setUpdatedAt(Instant.now());
                    repository.save(document);
                });
    }
}
