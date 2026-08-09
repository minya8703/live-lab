package com.minyaryung.livelab.application.blog;

import com.minyaryung.livelab.domain.blog.BlogRepository;
import com.minyaryung.livelab.domain.blog.BlogRequest;
import com.minyaryung.livelab.infra.common.MarkdownService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BlogServiceTest {

    @Test
    void generatedSlugNeverExceedsDatabaseColumnLength() {
        String slug = BlogService.toSlug("word ".repeat(100));

        assertThat(slug).hasSizeLessThanOrEqualTo(200);
        assertThat(slug).doesNotEndWith("-");
    }

    @Test
    void rejectsDuplicateSlugBeforeRenderingOrSaving() {
        BlogRepository repo = mock(BlogRepository.class);
        MarkdownService markdown = mock(MarkdownService.class);
        when(repo.existsBySlug("duplicate")).thenReturn(true);
        BlogService service = new BlogService(repo, markdown);

        assertThatThrownBy(() -> service.create(request("duplicate")))
                .isInstanceOf(DuplicateBlogSlugException.class);
        verify(markdown, never()).render(any());
        verify(repo, never()).save(any());
    }

    @Test
    void translatesUniqueConstraintRaceToConflictException() {
        BlogRepository repo = mock(BlogRepository.class);
        MarkdownService markdown = mock(MarkdownService.class);
        when(repo.existsBySlug("race")).thenReturn(false);
        when(markdown.render("본문")).thenReturn("<p>본문</p>");
        when(repo.save(any())).thenThrow(new DataIntegrityViolationException("unique constraint"));
        BlogService service = new BlogService(repo, markdown);

        assertThatThrownBy(() -> service.create(request("race")))
                .isInstanceOf(DuplicateBlogSlugException.class)
                .hasMessage("이미 사용 중인 slug입니다.");
    }

    @Test
    void missingUpdateAndDeleteUseNotFoundException() {
        BlogRepository repo = mock(BlogRepository.class);
        when(repo.findBySlug("missing")).thenReturn(Optional.empty());
        BlogService service = new BlogService(repo, mock(MarkdownService.class));

        assertThatThrownBy(() -> service.update("missing", request("new-slug")))
                .isInstanceOf(BlogPostNotFoundException.class);
        assertThatThrownBy(() -> service.delete("missing"))
                .isInstanceOf(BlogPostNotFoundException.class);
    }

    private static BlogRequest request(String slug) {
        return new BlogRequest(slug, "제목", null, "본문", null, null, false);
    }
}
