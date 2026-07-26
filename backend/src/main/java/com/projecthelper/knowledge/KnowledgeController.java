package com.projecthelper.knowledge;

import com.projecthelper.common.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class KnowledgeController {
    private final KnowledgeService knowledgeService;

    @GetMapping("/api/knowledge/documents")
    public ApiResponse<List<KnowledgeDocument>> list() { return ApiResponse.success(knowledgeService.list()); }

    @GetMapping("/api/knowledge/documents/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable String id) {
        KnowledgeDocument document = knowledgeService.get(id);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(document.getOriginalFilename(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(knowledgeService.download(id));
    }

    @PostMapping(value = "/api/admin/knowledge/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<KnowledgeDocument>> upload(@RequestParam @NotBlank String title,
                                                                 @RequestParam(required = false) String description,
                                                                 @RequestPart MultipartFile file) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(knowledgeService.upload(title, description, file)));
    }

    @PostMapping("/api/admin/knowledge/documents/{id}/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<KnowledgeDocument>> reindex(@PathVariable String id) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(knowledgeService.reindex(id)));
    }

    @DeleteMapping("/api/admin/knowledge/documents/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        knowledgeService.delete(id);
        return ApiResponse.success(null);
    }
}
