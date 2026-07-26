package com.projecthelper.user;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);
    List<User> findByRoleAndMajorIdOrderByRealName(UserRole role, String majorId);
    List<User> findByMentorIdOrderByRealName(String mentorId);
    List<User> findByMentorIdAndRealName(String mentorId, String realName);
    boolean existsByStudentNo(String studentNo);
    boolean existsByTeacherNo(String teacherNo);
}
