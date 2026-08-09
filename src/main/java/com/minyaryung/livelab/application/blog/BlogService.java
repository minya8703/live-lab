package com.minyaryung.livelab.application.blog;

import com.minyaryung.livelab.domain.blog.*;
import com.minyaryung.livelab.infra.common.MarkdownService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;

import java.text.Normalizer;
import java.util.regex.Pattern;

@Service
public class BlogService {

    private final BlogRepository repo;
    private final MarkdownService markdown;

    public BlogService(BlogRepository repo, MarkdownService markdown) {
        this.repo = repo;
        this.markdown = markdown;
    }

    public Page<BlogDto> listPublished(int page, int size) {
        return repo.findByPublishedTrueOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(this::toDto);
    }

    public Page<BlogDto> listAll(int page, int size) {
        return repo.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(this::toDto);
    }

    public BlogDto findBySlug(String slug) {
        return repo.findBySlug(slug).map(this::toDto).orElse(null);
    }

    public BlogDto create(BlogRequest req) {
        var post = new BlogPost();
        String newSlug = resolveSlug(req);
        if (repo.existsBySlug(newSlug)) throw new DuplicateBlogSlugException();
        applyRequest(post, req, newSlug);
        return save(post);
    }

    public BlogDto update(String slug, BlogRequest req) {
        var post = repo.findBySlug(slug).orElseThrow(() ->
                new BlogPostNotFoundException());
        String newSlug = resolveSlug(req);
        if (!newSlug.equals(post.getSlug()) && repo.existsBySlug(newSlug))
            throw new DuplicateBlogSlugException();
        applyRequest(post, req, newSlug);
        return save(post);
    }

    public void delete(String slug) {
        var post = repo.findBySlug(slug).orElseThrow(BlogPostNotFoundException::new);
        repo.delete(post);
    }

    private void applyRequest(BlogPost post, BlogRequest req, String slug) {
        post.setTitle(req.title());
        post.setSlug(slug);
        post.setSummary(req.summary());
        post.setContent(req.content());
        post.setHtmlContent(markdown.render(req.content()));
        post.setThumbnailUrl(req.thumbnailUrl());
        post.setTags(req.tags());
        post.setPublished(req.published());
    }

    private BlogDto save(BlogPost post) {
        try {
            return toDto(repo.save(post));
        } catch (DataIntegrityViolationException ex) {
            // exists 확인 이후의 동시 요청 race도 DB unique constraint로 최종 방어한다.
            throw new DuplicateBlogSlugException();
        }
    }

    private static String resolveSlug(BlogRequest req) {
        return req.slug() != null ? req.slug() : toSlug(req.title());
    }

    private BlogDto toDto(BlogPost post) {
        return new BlogDto(
                post.getId(), post.getSlug(), post.getTitle(), post.getSummary(),
                post.getContent(), post.getHtmlContent(), post.getThumbnailUrl(),
                post.getTags(), post.isPublished(),
                post.getCreatedAt().toString(), post.getUpdatedAt().toString());
    }

    private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9\\uAC00-\\uD7A3-]");
    private static final Pattern MULTI_DASH = Pattern.compile("-{2,}");

    static String toSlug(String title) {
        String s = Normalizer.normalize(title.toLowerCase().trim(), Normalizer.Form.NFC);
        s = s.replaceAll("\\s+", "-");
        s = NON_SLUG.matcher(s).replaceAll("");
        s = MULTI_DASH.matcher(s).replaceAll("-");
        s = s.replaceAll("^-|-$", "");
        if (s.isEmpty()) return "post";
        if (s.length() > 200) s = s.substring(0, 200).replaceAll("-+$", "");
        return s.isEmpty() ? "post" : s;
    }
}
