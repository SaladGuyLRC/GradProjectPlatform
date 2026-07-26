package com.projecthelper.user;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@Document("users")
public class User {
    @Id
    private String id;
    @Indexed(unique = true)
    private String username;
    private String passwordHash;
    private String realName;
    private UserRole role;
    @Indexed(unique = true, sparse = true)
    private String studentNo;
    @Indexed(unique = true, sparse = true)
    private String teacherNo;
    private String collegeId;
    @Indexed
    private String majorId;
    @Indexed
    private String mentorId;
    private UserStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
