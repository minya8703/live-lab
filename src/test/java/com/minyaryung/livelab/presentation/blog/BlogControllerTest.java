package com.minyaryung.livelab.presentation.blog;

import com.minyaryung.livelab.application.blog.BlogService;
import com.minyaryung.livelab.application.blog.BlogPostNotFoundException;
import com.minyaryung.livelab.domain.blog.FileStorage;
import com.minyaryung.livelab.infra.security.AdminAuditLogger;
import com.minyaryung.livelab.infra.common.BlogUploadRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class BlogControllerTest {

    @Test
    void recordsOnlyTheDeleteActionAfterSuccessfulDeletion() {
        BlogService service = mock(BlogService.class);
        AdminAuditLogger auditLogger = mock(AdminAuditLogger.class);
        BlogController controller = new BlogController(
                service, mock(FileStorage.class), auditLogger, mock(BlogUploadRateLimiter.class));

        controller.delete("public-slug");

        verify(service).delete("public-slug");
        verify(auditLogger).success(AdminAuditLogger.Action.BLOG_DELETE);
    }

    @Test
    void doesNotRecordAuditEventWhenDeletionFails() {
        BlogService service = mock(BlogService.class);
        AdminAuditLogger auditLogger = mock(AdminAuditLogger.class);
        doThrow(new BlogPostNotFoundException()).when(service).delete("missing");
        BlogController controller = new BlogController(
                service, mock(FileStorage.class), auditLogger, mock(BlogUploadRateLimiter.class));

        assertThatThrownBy(() -> controller.delete("missing"))
                .isInstanceOf(BlogPostNotFoundException.class);
        verify(auditLogger, never()).success(any());
    }

    @Test
    void rejectsInvalidPageBeforeQueryingDatabase() {
        BlogService service = mock(BlogService.class);
        BlogController controller = new BlogController(service, mock(FileStorage.class),
                mock(AdminAuditLogger.class), mock(BlogUploadRateLimiter.class));

        assertThatThrownBy(() -> controller.list(-1, 12))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(400);
        verify(service, never()).listPublished(anyInt(), anyInt());
    }

    @Test
    void rejectsRateLimitedUploadBeforeCallingStorage() throws Exception {
        FileStorage storage = mock(FileStorage.class);
        BlogUploadRateLimiter limiter = mock(BlogUploadRateLimiter.class);
        when(limiter.tryAcquire("203.0.113.10")).thenReturn(false);
        BlogController controller = new BlogController(mock(BlogService.class), storage,
                mock(AdminAuditLogger.class), limiter);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/blog/upload");
        request.setRemoteAddr("203.0.113.10");
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", new byte[]{1});

        assertThatThrownBy(() -> controller.upload(file, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(429);
        verify(storage, never()).upload(any());
    }
}
