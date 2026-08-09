package com.projecthelper.project;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectSubmissionRepository extends MongoRepository<ProjectSubmission, String> {
    List<ProjectSubmission> findByProjectIdOrderByVersionDesc(String projectId);
    Optional<ProjectSubmission> findTopByProjectIdOrderByVersionDesc(String projectId);
    Optional<ProjectSubmission> findByProjectIdAndSha256(String projectId, String sha256);
}
