package com.projecthelper.project;

import com.projecthelper.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;

    @GetMapping("/api/projects/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<GraduationProject> me() { return ApiResponse.success(projectService.myProject()); }

    @PostMapping("/api/projects/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<GraduationProject> create(@Valid @RequestBody ProjectRequest request) {
        return ApiResponse.success(projectService.create(request.command()));
    }

    @PutMapping("/api/projects/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<GraduationProject> update(@Valid @RequestBody ProjectRequest request) {
        return ApiResponse.success(projectService.update(request.command()));
    }

    @GetMapping("/api/mentor/students/{studentId}/project")
    @PreAuthorize("hasRole('MENTOR')")
    public ApiResponse<GraduationProject> mentorView(@PathVariable String studentId) {
        return ApiResponse.success(projectService.mentorView(studentId));
    }

    public record ProjectRequest(@NotBlank String title, String summary, List<String> techStack,
                                 String repositoryUrl, @NotNull ProjectStatus status,
                                 LocalDate startDate, LocalDate plannedEndDate) {
        ProjectService.ProjectCommand command() {
            return new ProjectService.ProjectCommand(title, summary, techStack == null ? List.of() : techStack,
                    repositoryUrl, status, startDate, plannedEndDate);
        }
    }
}
