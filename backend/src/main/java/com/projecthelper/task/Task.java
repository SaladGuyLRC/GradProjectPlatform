package com.projecthelper.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@Document("tasks")
@CompoundIndex(name = "student_status_deadline", def = "{'studentId':1,'status':1,'deadlineAt':1}")
public class Task {
    @Id private String id;
    private String projectId;
    @Indexed private String studentId;
    @Indexed private String creatorId;
    @JsonIgnore
    @Indexed(unique = true, sparse = true)
    private String sourceDraftId;
    private String title;
    private String description;
    private TaskType type;
    private TaskPriority priority;
    private TaskStatus status;
    private Instant deadlineAt;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
