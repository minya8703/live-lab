package com.minyaryung.livelab.infra.common;

import com.minyaryung.livelab.application.blog.BlogPostNotFoundException;
import com.minyaryung.livelab.application.blog.DuplicateBlogSlugException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    @Test
    void mapsBlogConsistencyFailuresToMeaningfulStatuses() {
        assertThat(handler.handleBlogNotFound(new BlogPostNotFoundException()).getStatusCode().value())
                .isEqualTo(404);
        assertThat(handler.handleDuplicateBlogSlug(new DuplicateBlogSlugException()).getStatusCode().value())
                .isEqualTo(409);
    }
}
