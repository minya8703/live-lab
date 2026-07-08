package com.minyaryung.livelab.domain.blog;

public record BlogDto(
        Long id, String slug, String title, String summary,
        String content, String htmlContent, String thumbnailUrl,
        String tags, boolean published, String createdAt, String updatedAt
) {}
