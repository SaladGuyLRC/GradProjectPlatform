package com.projecthelper.knowledge;

import com.projecthelper.common.BusinessException;
import com.projecthelper.knowledge.parser.DocumentParserRegistry;
import com.projecthelper.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
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
public class KnowledgeService {
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeIndexingService indexingService;
    private final RedisVectorStore vectorStore;
    private final KnowledgeProperties properties;
    private final DocumentParserRegistry parserRegistry;
    private final CurrentUserService currentUserService;

    public KnowledgeDocument upload(String title, String description, MultipartFile file) {
        // 知识库只接收公开 PDF；保存原文件后异步切块、向量化并写入 Redis Stack。
        if (file.isEmpty()) throw BusinessException.badRequest("The PDF file cannot be empty");
        if (file.getSize() > 20L * 1024 * 1024) throw BusinessException.badRequest("The PDF cannot exceed 20 MB");
        String original = file.getOriginalFilename() == null ? "document.pdf" : file.getOriginalFilename();
        parserRegistry.requireParser(original, file.getContentType());
        String sha = sha256(file);
        if (documentRepository.findBySha256(sha).isPresent()) throw BusinessException.conflict("This PDF has already been uploaded");
        String id = UUID.randomUUID().toString();
        String storedFilename = id + ".pdf";
        Path root = Path.of(properties.getStoragePath()).toAbsolutePath().normalize();
        Path target = root.resolve(storedFilename).normalize();
        if (!target.startsWith(root)) throw BusinessException.badRequest("Invalid file path");
        try {
            Files.createDirectories(root);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to save PDF", exception);
        }
        Instant now = Instant.now();
        KnowledgeDocument document = KnowledgeDocument.builder().id(id).title(title.trim()).description(description)
                .originalFilename(original).storedFilename(storedFilename).storedPath(target.toString())
                .mimeType(file.getContentType()).fileSize(file.getSize()).sha256(sha)
                .uploaderId(currentUserService.require().getId()).status(KnowledgeStatus.UPLOADED)
                .createdAt(now).updatedAt(now).build();
        documentRepository.save(document);
        indexingService.indexAsync(document.getId());
        return document;
    }

    public List<KnowledgeDocument> list() {
        return documentRepository.findAllByOrderByCreatedAtDesc();
    }

    public KnowledgeDocument get(String id) {
        return documentRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Knowledge document not found"));
    }

    public Resource download(String id) {
        try {
            Resource resource = new UrlResource(Path.of(get(id).getStoredPath()).toUri());
            if (!resource.exists()) throw BusinessException.notFound("The original PDF does not exist");
            return resource;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to read PDF", exception);
        }
    }

    public KnowledgeDocument reindex(String id) {
        // 重建索引保留 MongoDB 文档元数据，仅重新生成 Redis 中的向量片段。
        KnowledgeDocument document = get(id);
        if (document.getStatus() == KnowledgeStatus.PROCESSING) throw BusinessException.conflict("The document is already being processed");
        document.setStatus(KnowledgeStatus.UPLOADED);
        document.setFailureReason(null);
        document.setUpdatedAt(Instant.now());
        documentRepository.save(document);
        indexingService.indexAsync(id);
        return document;
    }

    public void delete(String id) {
        KnowledgeDocument document = get(id);
        if (document.getStatus() == KnowledgeStatus.PROCESSING) throw BusinessException.conflict("The document is being processed and cannot be deleted yet");
        vectorStore.deleteDocument(id);
        try {
            Files.deleteIfExists(Path.of(document.getStoredPath()));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to delete original PDF", exception);
        }
        documentRepository.delete(document);
    }

    public List<RedisVectorStore.SearchHit> search(String question) {
        // 对外搜索固定返回少量候选片段，供普通知识库页面或 AI 工具继续组织答案。
        if (question == null || question.isBlank()) throw BusinessException.badRequest("The question cannot be empty");
        return vectorStore.search(question.trim(), 3);
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
}
