package com.projecthelper.user;

import com.projecthelper.auth.AuthController.UserView;
import com.projecthelper.common.ApiResponse;
import com.projecthelper.common.BusinessException;
import com.projecthelper.organization.OrganizationRepository;
import com.projecthelper.organization.OrganizationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public ApiResponse<List<UserView>> list() {
        return ApiResponse.success(userRepository.findAll().stream().map(UserView::of).toList());
    }

    @PostMapping
    public ApiResponse<UserView> create(@Valid @RequestBody UserRequest request) {
        validate(request, null);
        if (userRepository.findByUsername(request.username()).isPresent()) throw BusinessException.conflict("Username already exists");
        Instant now = Instant.now();
        User user = User.builder().username(request.username().trim())
                .passwordHash(passwordEncoder.encode(request.initialPassword()))
                .realName(request.realName().trim()).role(request.role()).studentNo(blankToNull(request.studentNo()))
                .teacherNo(blankToNull(request.teacherNo())).collegeId(request.collegeId()).majorId(request.majorId())
                .mentorId(request.mentorId()).status(UserStatus.ACTIVE).createdAt(now).updatedAt(now).build();
        return ApiResponse.success(UserView.of(userRepository.save(user)));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserView> update(@PathVariable String id, @Valid @RequestBody UserRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> BusinessException.notFound("User not found"));
        validate(request, id);
        user.setRealName(request.realName().trim());
        user.setRole(request.role());
        user.setStudentNo(blankToNull(request.studentNo()));
        user.setTeacherNo(blankToNull(request.teacherNo()));
        user.setCollegeId(request.collegeId());
        user.setMajorId(request.majorId());
        user.setMentorId(request.mentorId());
        if (request.initialPassword() != null && !request.initialPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.initialPassword()));
        }
        user.setUpdatedAt(Instant.now());
        return ApiResponse.success(UserView.of(userRepository.save(user)));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> status(@PathVariable String id, @RequestBody StatusRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> BusinessException.notFound("User not found"));
        user.setStatus(request.status());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        return ApiResponse.success(null);
    }

    private void validate(UserRequest request, String updatingId) {
        var college = organizationRepository.findById(request.collegeId())
                .orElseThrow(() -> BusinessException.badRequest("College not found"));
        var major = organizationRepository.findById(request.majorId())
                .orElseThrow(() -> BusinessException.badRequest("Major not found"));
        if (college.getType() != OrganizationType.COLLEGE || major.getType() != OrganizationType.MAJOR
                || !college.getId().equals(major.getParentId())) throw BusinessException.badRequest("College and major do not match");
        if (request.role() == UserRole.STUDENT) {
            if (request.studentNo() == null || request.studentNo().isBlank()) throw BusinessException.badRequest("Students must provide a student number");
            User mentor = userRepository.findById(request.mentorId()).orElseThrow(() -> BusinessException.badRequest("Mentor not found"));
            if (mentor.getRole() != UserRole.MENTOR || !request.majorId().equals(mentor.getMajorId())) {
                throw BusinessException.badRequest("A student's mentor must be in the same major");
            }
        }
        userRepository.findByUsername(request.username()).ifPresent(existing -> {
        if (!existing.getId().equals(updatingId)) throw BusinessException.conflict("Username already exists");
        });
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public record UserRequest(@NotBlank String username, String initialPassword, @NotBlank String realName,
                              @NotNull UserRole role, String studentNo, String teacherNo,
                              @NotBlank String collegeId, @NotBlank String majorId, String mentorId) {}
    public record StatusRequest(@NotNull UserStatus status) {}
}
