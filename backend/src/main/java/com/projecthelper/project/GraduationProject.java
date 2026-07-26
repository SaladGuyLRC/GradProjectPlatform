package com.projecthelper.project;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@Document("graduation_projects")
public class GraduationProject {
    @Id private String id;
    @Indexed(unique = true) private String studentId;
    @Indexed private String mentorId;
    private String title;
    private String summary;
    private List<String> techStack;
    private String repositoryUrl;
    private ProjectStatus status;
    private LocalDate startDate;
    private LocalDate plannedEndDate;
    private Instant createdAt;
    private Instant updatedAt;
}
