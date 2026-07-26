package com.projecthelper.dashboard;

import com.projecthelper.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StudentDashboardController {
    private final StudentDashboardService dashboardService;

    @GetMapping("/api/dashboard/student")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<StudentDashboardService.StudentDashboard> dashboard() {
        return ApiResponse.success(dashboardService.dashboard());
    }
}
