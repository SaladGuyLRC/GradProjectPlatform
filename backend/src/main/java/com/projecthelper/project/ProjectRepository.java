package com.projecthelper.project;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends MongoRepository<GraduationProject, String> {
    Optional<GraduationProject> findByStudentId(String studentId);
    List<GraduationProject> findByMentorId(String mentorId);
}
