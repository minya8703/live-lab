package com.minyaryung.livelab.infra.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AdminAuditLogger {

    private static final Logger log = LoggerFactory.getLogger(AdminAuditLogger.class);

    public void success(Action action) {
        // 이메일, JWT, slug, 본문, 원본 파일명은 감사 로그에 포함하지 않는다.
        log.info("admin_audit action={} outcome=success", action.value);
    }

    public enum Action {
        BLOG_CREATE("blog.create"),
        BLOG_UPDATE("blog.update"),
        BLOG_DELETE("blog.delete"),
        BLOG_UPLOAD("blog.upload");

        private final String value;

        Action(String value) {
            this.value = value;
        }
    }
}
