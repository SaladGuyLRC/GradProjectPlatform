package com.projecthelper.project;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@Document("project_submissions")
public class ProjectSubmission {
    @Id private String id;
    @Indexed private String projectId;
    @Indexed private String studentId;
    private String originalFilename;
    private String storedFilename;
    private String storedPath;
    private String mimeType;
    private long fileSize;
    private String sha256;
    private int version;
    private Instant createdAt;
}
