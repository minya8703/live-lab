package com.minyaryung.livelab.application.blog;

public class BlogPostNotFoundException extends RuntimeException {
    public BlogPostNotFoundException() {
        super("블로그 글을 찾을 수 없습니다.");
    }
}
