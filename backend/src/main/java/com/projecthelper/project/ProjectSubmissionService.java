package com.projecthelper.project;

import com.projecthelper.common.BusinessException;
import com.projecthelper.security.CurrentUserService;
import com.projecthelper.user.User;
import com.projecthelper.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectSubmissionService {
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;

    private final ProjectSubmissionRepository submissionRepository;
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final CurrentUserService currentUserService;

    @Value("${app.project-submissions.storage-path:data/project-submissions}")
    private String storagePath;

    public List<SubmissionView> listForStudent() {
        User student = requireStudent();
        GraduationProject project = projectRepository.findByStudentId(student.getId()).orElse(null);
        return project == null ? List.of() : views(submissionRepository.findByProjectIdOrderByVersionDesc(project.getId()));
    }

    public SubmissionView uploadForStudent(MultipartFile file) {
        // 上传物绑定到当前学生项目，并通过大小、扩展名、文件头和 SHA-256 去重。
        User student = requireStudent();
        GraduationProject project = projectRepository.findByStudentId(student.getId())
                .orElseThrow(() -> BusinessException.badRequest("Create your project before uploading a submission"));
        validatePdf(file);
        String sha = sha256(file);
        if (submissionRepository.findByProjectIdAndSha256(project.getId(), sha).isPresent()) {
            throw BusinessException.conflict("This PDF has already been uploaded");
        }
        int version = submissionRepository.findTopByProjectIdOrderByVersionDesc(project.getId())
                .map(previous -> previous.getVersion() + 1).orElse(1);
        String id = UUID.randomUUID().toString();
        String original = originalFilename(file);
        Path root = Path.of(storagePath).toAbsolutePath().normalize();
        Path target = root.resolve(id + ".pdf").normalize();
        if (!target.startsWith(root)) throw BusinessException.badRequest("Invalid file path");
        try {
            Files.createDirectories(root);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to save project submission", exception);
        }
        ProjectSubmission submission = ProjectSubmission.builder().id(id).projectId(project.getId())
                .studentId(student.getId()).originalFilename(original).storedFilename(id + ".pdf")
                .storedPath(target.toString()).mimeType("application/pdf").fileSize(file.getSize()).sha256(sha)
                .version(version).createdAt(Instant.now()).build();
        return view(submissionRepository.save(submission));
    }

    public List<SubmissionView> listForMentor(String studentId) {
        // 导师只能查看自己负责学生的提交物，具体关系由 ProjectService 再次确认。
        User mentor = currentUserService.require();
        User student = projectService.requireSupervisedStudent(mentor, studentId);
        GraduationProject project = projectRepository.findByStudentId(student.getId()).orElse(null);
        return project == null ? List.of() : views(submissionRepository.findByProjectIdOrderByVersionDesc(project.getId()));
    }

    public Resource downloadForStudent(String id) {
        User student = requireStudent();
        ProjectSubmission submission = findForStudent(id, student.getId());
        return resource(submission);
    }

    public Resource downloadForMentor(String studentId, String id) {
        User mentor = currentUserService.require();
        User student = projectService.requireSupervisedStudent(mentor, studentId);
        ProjectSubmission submission = findForStudent(id, student.getId());
        return resource(submission);
    }

    public SubmissionView getView(String id, String studentId) {
        return view(findForStudent(id, studentId));
    }

    private ProjectSubmission findForStudent(String id, String studentId) {
        ProjectSubmission submission = submissionRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Project submission not found"));
        if (!studentId.equals(submission.getStudentId())) throw BusinessException.forbidden("You cannot access this project submission");
        return submission;
    }

    private Resource resource(ProjectSubmission submission) {
        try {
            Resource resource = new UrlResource(Path.of(submission.getStoredPath()).toUri());
            if (!resource.exists()) throw BusinessException.notFound("The original PDF does not exist");
            return resource;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to read project submission", exception);
        }
    }

    private void validatePdf(MultipartFile file) {
        // 仅检查真实 PDF 文件头和大小，不能只信任浏览器传来的 MIME 类型。
        if (file == null || file.isEmpty()) throw BusinessException.badRequest("The PDF file cannot be empty");
        if (file.getSize() > MAX_FILE_SIZE) throw BusinessException.badRequest("The PDF cannot exceed 20 MB");
        String filename = originalFilename(file);
        if (!filename.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) {
            throw BusinessException.badRequest("Only PDF files are accepted");
        }
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(5);
            if (header.length < 5 || header[0] != '%' || header[1] != 'P' || header[2] != 'D' || header[3] != 'F' || header[4] != '-') {
                throw BusinessException.badRequest("Only PDF files are accepted");
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to validate project submission", exception);
        }
    }

    private String originalFilename(MultipartFile file) {
        String filename = file.getOriginalFilename();
        return filename == null || filename.isBlank() ? "submission.pdf" : Path.of(filename).getFileName().toString();
    }

    private String sha256(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int length;
            while ((length = input.read(buffer)) > 0) digest.update(buffer, 0, length);
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to calculate file digest", exception);
        }
    }

    private User requireStudent() {
        User user = currentUserService.require();
        if (user.getRole() != UserRole.STUDENT) throw BusinessException.forbidden("Only students can manage project submissions");
        return user;
    }

    private List<SubmissionView> views(List<ProjectSubmission> submissions) { return submissions.stream().map(this::view).toList(); }
    private SubmissionView view(ProjectSubmission submission) {
        return new SubmissionView(submission.getId(), submission.getOriginalFilename(), submission.getFileSize(), submission.getVersion(), submission.getCreatedAt());
    }

    public record SubmissionView(String id, String originalFilename, long fileSize, int version, Instant uploadedAt) {}
}
