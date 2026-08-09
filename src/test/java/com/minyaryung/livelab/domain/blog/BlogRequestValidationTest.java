package com.minyaryung.livelab.domain.blog;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BlogRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsRequestWithinDocumentedLimits() {
        BlogRequest request = new BlogRequest(
                "valid-slug", "제목", "요약", "본문", null, "Java,Spring", false);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsBlankRequiredFieldsAndUnsafeSlugOrThumbnail() {
        BlogRequest request = new BlogRequest(
                "../Admin", " ", null, "", "javascript:alert(1)", null, false);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("slug", "title", "content", "thumbnailUrl");
    }

    @Test
    void rejectsOversizedContent() {
        BlogRequest request = new BlogRequest(
                null, "제목", null, "x".repeat(100_001), null, null, false);

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("content"));
    }
}
