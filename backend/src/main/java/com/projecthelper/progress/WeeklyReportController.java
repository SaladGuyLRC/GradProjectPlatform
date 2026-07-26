package com.projecthelper.progress;

import com.projecthelper.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class WeeklyReportController {
    private final WeeklyReportService reportService;

    @GetMapping("/api/weekly-reports/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<Page<WeeklyReport>> mine(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(reportService.myReports(page, size));
    }

    @PostMapping("/api/weekly-reports")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<WeeklyReport> create(@Valid @RequestBody ReportRequest request) {
        return ApiResponse.success(reportService.create(request.command()));
    }

    @GetMapping("/api/weekly-reports/{id}")
    public ApiResponse<WeeklyReport> get(@PathVariable String id) { return ApiResponse.success(reportService.get(id)); }

    @PutMapping("/api/weekly-reports/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<WeeklyReport> update(@PathVariable String id, @Valid @RequestBody ReportRequest request) {
        return ApiResponse.success(reportService.update(id, request.command()));
    }

    @DeleteMapping("/api/weekly-reports/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        reportService.delete(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/api/weekly-reports/{id}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<WeeklyReport> submit(@PathVariable String id) { return ApiResponse.success(reportService.submit(id)); }

    @GetMapping("/api/mentor/weekly-reports")
    @PreAuthorize("hasRole('MENTOR')")
    public ApiResponse<Page<WeeklyReport>> mentorReports(@RequestParam(required = false) WeeklyReportStatus status,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(reportService.mentorReports(status, page, size));
    }

    @PutMapping("/api/mentor/weekly-reports/{id}/review")
    @PreAuthorize("hasRole('MENTOR')")
    public ApiResponse<WeeklyReport> review(@PathVariable String id, @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.success(reportService.review(id, request.content()));
    }

    public record ReportRequest(@NotNull LocalDate weekStart, @NotBlank String completedWork,
                                String currentProblems, @NotBlank String nextWeekPlan,
                                @Min(0) @Max(100) int progressPercentage) {
        WeeklyReportService.ReportCommand command() {
            return new WeeklyReportService.ReportCommand(weekStart, completedWork, currentProblems,
                    nextWeekPlan, progressPercentage);
        }
    }
    public record ReviewRequest(@NotBlank String content) {}
}
