package com.minyaryung.livelab.domain.blog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BlogRequest(
        @Size(max = 200, message = "slug는 200자 이하여야 합니다.")
        @Pattern(regexp = "^[a-z0-9가-힣]+(?:-[a-z0-9가-힣]+)*$",
                message = "slug는 한글·영문 소문자·숫자와 단일 하이픈만 사용할 수 있습니다.")
        String slug,
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 300, message = "제목은 300자 이하여야 합니다.")
        String title,
        @Size(max = 500, message = "요약은 500자 이하여야 합니다.")
        String summary,
        @NotBlank(message = "본문은 필수입니다.")
        @Size(max = 100_000, message = "본문은 100,000자 이하여야 합니다.")
        String content,
        @Size(max = 500, message = "썸네일 URL은 500자 이하여야 합니다.")
        @Pattern(regexp = "^https?://[^\\s]+$", message = "썸네일은 HTTP(S) URL이어야 합니다.")
        String thumbnailUrl,
        @Size(max = 500, message = "태그는 500자 이하여야 합니다.")
        String tags,
        boolean published
) {}
