package com.minyaryung.livelab.application.blog;

public class DuplicateBlogSlugException extends RuntimeException {
    public DuplicateBlogSlugException() {
        super("이미 사용 중인 slug입니다.");
    }
}
