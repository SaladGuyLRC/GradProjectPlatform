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
        if (file.isEmpty()) throw BusinessException.badRequest("PDF文件不能为空");
        if (file.getSize() > 20L * 1024 * 1024) throw BusinessException.badRequest("PDF不能超过20MB");
        String original = file.getOriginalFilename() == null ? "document.pdf" : file.getOriginalFilename();
        parserRegistry.requireParser(original, file.getContentType());
        String sha = sha256(file);
        if (documentRepository.findBySha256(sha).isPresent()) throw BusinessException.conflict("该PDF已经上传");
        String id = UUID.randomUUID().toString();
        String storedFilename = id + ".pdf";
        Path root = Path.of(properties.getStoragePath()).toAbsolutePath().normalize();
        Path target = root.resolve(storedFilename).normalize();
        if (!target.startsWith(root)) throw BusinessException.badRequest("文件路径非法");
        try {
            Files.createDirectories(root);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("保存PDF失败", exception);
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
        return documentRepository.findById(id).orElseThrow(() -> BusinessException.notFound("知识文档不存在"));
    }

    public Resource download(String id) {
        try {
            Resource resource = new UrlResource(Path.of(get(id).getStoredPath()).toUri());
            if (!resource.exists()) throw BusinessException.notFound("原始PDF不存在");
            return resource;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("读取PDF失败", exception);
        }
    }

    public KnowledgeDocument reindex(String id) {
        KnowledgeDocument document = get(id);
        if (document.getStatus() == KnowledgeStatus.PROCESSING) throw BusinessException.conflict("文档正在处理");
        document.setStatus(KnowledgeStatus.UPLOADED);
        document.setFailureReason(null);
        document.setUpdatedAt(Instant.now());
        documentRepository.save(document);
        indexingService.indexAsync(id);
        return document;
    }

    public void delete(String id) {
        KnowledgeDocument document = get(id);
        if (document.getStatus() == KnowledgeStatus.PROCESSING) throw BusinessException.conflict("文档正在处理，暂不能删除");
        vectorStore.deleteDocument(id);
        try {
            Files.deleteIfExists(Path.of(document.getStoredPath()));
        } catch (Exception exception) {
            throw new IllegalStateException("删除原始PDF失败", exception);
        }
        documentRepository.delete(document);
    }

    public List<RedisVectorStore.SearchHit> search(String question) {
        if (question == null || question.isBlank()) throw BusinessException.badRequest("问题不能为空");
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
            throw new IllegalStateException("计算文件摘要失败", exception);
        }
    }
}
