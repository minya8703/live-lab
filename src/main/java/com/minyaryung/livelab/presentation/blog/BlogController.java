package com.minyaryung.livelab.presentation.blog;

import com.minyaryung.livelab.application.blog.BlogService;
import com.minyaryung.livelab.domain.blog.BlogDto;
import com.minyaryung.livelab.domain.blog.BlogRequest;
import com.minyaryung.livelab.domain.blog.FileStorage;
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

    public BlogController(BlogService service,
                          @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
                          @org.springframework.lang.Nullable FileStorage storage) {
        this.service = service;
        this.storage = storage;
    }

    @GetMapping
    public Page<BlogDto> list(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "12") int size) {
        return service.listPublished(page, Math.min(size, 50));
    }

    @GetMapping("/{slug}")
    public BlogDto detail(@PathVariable String slug) {
        var dto = service.findBySlug(slug);
        if (dto == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return dto;
    }

    @GetMapping("/admin/list")
    public Page<BlogDto> adminList(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        return service.listAll(page, Math.min(size, 50));
    }

    @PostMapping
    public ResponseEntity<BlogDto> create(@RequestBody BlogRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{slug}")
    public BlogDto update(@PathVariable String slug, @RequestBody BlogRequest req) {
        return service.update(slug, req);
    }

    @DeleteMapping("/{slug}")
    public ResponseEntity<Void> delete(@PathVariable String slug) {
        service.delete(slug);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/upload")
    public Map<String, String> upload(@RequestPart("file") MultipartFile file) throws IOException {
        if (storage == null)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "S3 storage not configured");
        if (file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty file");
        return Map.of("url", storage.upload(file));
    }
}
