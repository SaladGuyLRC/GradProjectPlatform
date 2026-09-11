package com.projecthelper.auth;

import com.projecthelper.common.ApiResponse;
import com.projecthelper.common.BusinessException;
import com.projecthelper.security.CurrentUserService;
import com.projecthelper.security.JwtProperties;
import com.projecthelper.security.JwtService;
import com.projecthelper.user.User;
import com.projecthelper.user.UserRepository;
import com.projecthelper.user.UserStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final CurrentUserService currentUserService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // 登录只返回短期 JWT；密码本身只与数据库中的 BCrypt 哈希比较，不会被保存或回传。
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid username or password"));
        if (user.getStatus() != UserStatus.ACTIVE || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid username or password");
        }
        return ApiResponse.success(new LoginResponse(jwtService.create(user.getId()), jwtProperties.getExpireSeconds(), UserView.of(user)));
    }

    @GetMapping("/me")
    public ApiResponse<UserView> me() {
        return ApiResponse.success(UserView.of(currentUserService.require()));
    }

    @PutMapping("/password")
    public ApiResponse<Void> updatePassword(@Valid @RequestBody PasswordRequest request) {
        // 修改密码必须由当前登录用户发起，并先验证旧密码，管理员不能代替用户修改。
        User user = currentUserService.require();
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw BusinessException.badRequest("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        return ApiResponse.success(null);
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record PasswordRequest(@NotBlank String oldPassword, @NotBlank String newPassword) {}
    public record LoginResponse(String token, long expiresIn, UserView user) {}
    public record UserView(String id, String username, String realName, String role, String studentNo,
                           String teacherNo, String collegeId, String majorId, String mentorId, String status) {
        public static UserView of(User user) {
            return new UserView(user.getId(), user.getUsername(), user.getRealName(), user.getRole().name(),
                    user.getStudentNo(), user.getTeacherNo(), user.getCollegeId(), user.getMajorId(), user.getMentorId(),
                    user.getStatus() == null ? null : user.getStatus().name());
        }
    }
}
