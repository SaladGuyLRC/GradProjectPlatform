package com.projecthelper.knowledge;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("app.knowledge")
public class KnowledgeProperties {
    private String storagePath;
    private String embeddingModel;
    private int embeddingDimension;
    private int chunkSize;
    private int chunkOverlap;
}
