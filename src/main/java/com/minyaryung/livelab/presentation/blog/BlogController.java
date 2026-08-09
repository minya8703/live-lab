package com.minyaryung.livelab.presentation.blog;

import com.minyaryung.livelab.application.blog.BlogService;
import com.minyaryung.livelab.domain.blog.BlogDto;
import com.minyaryung.livelab.domain.blog.BlogRequest;
import com.minyaryung.livelab.domain.blog.FileStorage;
import com.minyaryung.livelab.infra.security.AdminAuditLogger;
import com.minyaryung.livelab.infra.common.BlogUploadRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/blog")
public class BlogController {

    private final BlogService service;
    private final FileStorage storage;
    private final AdminAuditLogger auditLogger;
    private final BlogUploadRateLimiter uploadRateLimiter;

    public BlogController(BlogService service,
                          @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
                          @org.springframework.lang.Nullable FileStorage storage,
                          AdminAuditLogger auditLogger,
                          BlogUploadRateLimiter uploadRateLimiter) {
        this.service = service;
        this.storage = storage;
        this.auditLogger = auditLogger;
        this.uploadRateLimiter = uploadRateLimiter;
    }

    @GetMapping
    public Page<BlogDto> list(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "12") int size) {
        validatePage(page, size);
        return service.listPublished(page, size);
    }

    @GetMapping("/{slug}")
    public BlogDto detail(@PathVariable String slug) {
        validatePathSlug(slug);
        var dto = service.findBySlug(slug);
        if (dto == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return dto;
    }

    @GetMapping("/admin/list")
    public Page<BlogDto> adminList(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        validatePage(page, size);
        return service.listAll(page, size);
    }

    @PostMapping
    public ResponseEntity<BlogDto> create(@Valid @RequestBody BlogRequest req) {
        BlogDto created = service.create(req);
        auditLogger.success(AdminAuditLogger.Action.BLOG_CREATE);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{slug}")
    public BlogDto update(@PathVariable String slug, @Valid @RequestBody BlogRequest req) {
        validatePathSlug(slug);
        BlogDto updated = service.update(slug, req);
        auditLogger.success(AdminAuditLogger.Action.BLOG_UPDATE);
        return updated;
    }

    @DeleteMapping("/{slug}")
    public ResponseEntity<Void> delete(@PathVariable String slug) {
        validatePathSlug(slug);
        service.delete(slug);
        auditLogger.success(AdminAuditLogger.Action.BLOG_DELETE);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/upload")
    public Map<String, String> upload(@RequestPart("file") MultipartFile file,
                                      HttpServletRequest request) throws IOException {
        if (storage == null)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "S3 storage not configured");
        if (file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty file");
        if (!uploadRateLimiter.tryAcquire(request.getRemoteAddr()))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "이미지 업로드 한도를 초과했습니다. 잠시 후 다시 시도해 주세요.");
        String url = storage.upload(file);
        auditLogger.success(AdminAuditLogger.Action.BLOG_UPLOAD);
        return Map.of("url", url);
    }

    private static void validatePage(int page, int size) {
        if (page < 0 || page > 1_000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page는 0~1000 사이여야 합니다.");
        if (size < 1 || size > 50)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size는 1~50 사이여야 합니다.");
    }

    private static void validatePathSlug(String slug) {
        if (slug == null || slug.length() > 200 || !slug.matches("^[a-z0-9가-힣]+(?:-[a-z0-9가-힣]+)*$"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바르지 않은 slug입니다.");
    }
}
