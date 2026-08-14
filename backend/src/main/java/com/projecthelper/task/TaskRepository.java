package com.projecthelper.task;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends MongoRepository<Task, String> {
    Optional<Task> findBySourceDraftId(String sourceDraftId);
    Page<Task> findByStudentId(String studentId, Pageable pageable);
    Page<Task> findByStudentIdAndStatus(String studentId, TaskStatus status, Pageable pageable);
    List<Task> findByStudentIdAndStatusInAndDeadlineAtBetweenOrderByDeadlineAtAsc(
            String studentId, Collection<TaskStatus> statuses, Instant start, Instant end);
    List<Task> findByStudentIdAndStatusInAndDeadlineAtBeforeOrderByDeadlineAtAsc(
            String studentId, Collection<TaskStatus> statuses, Instant end);
}
