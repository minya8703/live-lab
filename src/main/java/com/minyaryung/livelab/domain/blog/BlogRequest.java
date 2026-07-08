package com.minyaryung.livelab.domain.blog;

public record BlogRequest(
        String slug, String title, String summary, String content,
        String thumbnailUrl, String tags, boolean published
) {}
