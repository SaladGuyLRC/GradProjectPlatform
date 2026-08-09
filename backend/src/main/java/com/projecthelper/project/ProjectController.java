package com.projecthelper.project;

import com.projecthelper.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import com.projecthelper.security.CurrentUserService;

import java.nio.charset.StandardCharsets;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;
    private final ProjectSubmissionService submissionService;
    private final CurrentUserService currentUserService;

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

    @GetMapping("/api/projects/me/submissions")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<ProjectSubmissionService.SubmissionView>> submissions() {
        return ApiResponse.success(submissionService.listForStudent());
    }

    @PostMapping(value = "/api/projects/me/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<ProjectSubmissionService.SubmissionView>> upload(@RequestPart MultipartFile file) {
        return ResponseEntity.status(202).body(ApiResponse.success(submissionService.uploadForStudent(file)));
    }

    @GetMapping("/api/projects/me/submissions/{id}/download")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Resource> download(@PathVariable String id) {
        ProjectSubmissionService.SubmissionView view = submissionService.getView(id, currentStudentId());
        return fileResponse(view, submissionService.downloadForStudent(id));
    }

    @GetMapping("/api/mentor/students/{studentId}/project")
    @PreAuthorize("hasRole('MENTOR')")
    public ApiResponse<GraduationProject> mentorView(@PathVariable String studentId) {
        return ApiResponse.success(projectService.mentorView(studentId));
    }

    @GetMapping("/api/mentor/students/{studentId}/project/submissions")
    @PreAuthorize("hasRole('MENTOR')")
    public ApiResponse<List<ProjectSubmissionService.SubmissionView>> mentorSubmissions(@PathVariable String studentId) {
        return ApiResponse.success(submissionService.listForMentor(studentId));
    }

    @GetMapping("/api/mentor/students/{studentId}/project/submissions/{id}/download")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<Resource> mentorDownload(@PathVariable String studentId, @PathVariable String id) {
        ProjectSubmissionService.SubmissionView view = submissionService.listForMentor(studentId).stream()
                .filter(item -> item.id().equals(id)).findFirst()
                .orElseThrow(() -> com.projecthelper.common.BusinessException.notFound("Project submission not found"));
        return fileResponse(view, submissionService.downloadForMentor(studentId, id));
    }

    private String currentStudentId() {
        return currentUserService.require().getId();
    }

    private ResponseEntity<Resource> fileResponse(ProjectSubmissionService.SubmissionView view, Resource resource) {
        ContentDisposition disposition = ContentDisposition.attachment().filename(view.originalFilename(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString()).body(resource);
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
