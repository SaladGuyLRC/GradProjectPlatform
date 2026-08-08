package com.projecthelper.ai.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projecthelper.ai.AiActorResolver;
import com.projecthelper.progress.WeeklyReportService;
import com.projecthelper.project.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AcademicTools {
    private final AiActorResolver actorResolver;
    private final ProjectRepository projectRepository;
    private final WeeklyReportService reportService;
    private final ObjectMapper objectMapper;

    @Tool(description = "Find a student's graduation project record. A student can query only themselves; a mentor must name an assigned student.")
    public String getProject(@ToolParam(description = "Assigned student name for a mentor; omit for a student", required = false) String studentName) {
        var student = actorResolver.targetStudent(studentName, true);
        return json(projectRepository.findByStudentId(student.getId()).orElse(null));
    }

    @Tool(description = "Find the latest ten weekly reports. A student can query only themselves; a mentor must name an assigned student.")
    public String findWeeklyReports(@ToolParam(description = "Assigned student name for a mentor; omit for a student", required = false) String studentName) {
        var student = actorResolver.targetStudent(studentName, true);
        return json(reportService.reportsForAi(student.getId()));
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Failed to serialize academic data", exception); }
    }
}
