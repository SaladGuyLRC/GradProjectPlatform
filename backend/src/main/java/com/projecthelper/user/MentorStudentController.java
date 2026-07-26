package com.projecthelper.user;

import com.projecthelper.auth.AuthController.UserView;
import com.projecthelper.common.ApiResponse;
import com.projecthelper.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mentor/students")
@PreAuthorize("hasRole('MENTOR')")
@RequiredArgsConstructor
public class MentorStudentController {
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<UserView>> list() {
        User mentor = currentUserService.require();
        return ApiResponse.success(userRepository.findByMentorIdOrderByRealName(mentor.getId()).stream()
                .map(UserView::of).toList());
    }
}
