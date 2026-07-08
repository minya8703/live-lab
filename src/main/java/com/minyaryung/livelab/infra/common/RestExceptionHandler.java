package com.minyaryung.livelab.infra.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleStatus(ResponseStatusException ex) {
        log.info("client error {} — {}", ex.getStatusCode().value(), ex.getReason());
        return body(ex.getStatusCode(), reasonOrDefault(ex.getReason(), "\uc694\uccad \ucc98\ub9ac \uc2e4\ud328"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> handleNoResource(NoResourceFoundException ex) {
        log.debug("static resource not found: {}", ex.getResourcePath());
        return body(HttpStatus.NOT_FOUND, "\ub9ac\uc18c\uc2a4\ub97c \ucc3e\uc744 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<Map<String, String>> handleHttpClient(HttpClientErrorException ex) {
        int status = ex.getStatusCode().value();
        log.error("LLM 4xx — status={} body={}", status, safeBody(ex.getResponseBodyAsString()), ex);
        String msg = switch (status) {
            case 400 -> "LLM \uc694\uccad \ud615\uc2dd \uc624\ub958\uc785\ub2c8\ub2e4. \uc785\ub825\uc744 \uc9e7\uac8c \uc904\uc5ec \ub2e4\uc2dc \uc2dc\ub3c4\ud574 \uc8fc\uc138\uc694.";
            case 401, 403 -> "LLM \uc778\uc99d\uc5d0 \uc2e4\ud328\ud588\uc2b5\ub2c8\ub2e4. \uc11c\ubc84\uc758 API \ud0a4 \uc124\uc815\uc744 \ud655\uc778\ud574 \uc8fc\uc138\uc694.";
            case 404 -> "LLM \ubaa8\ub378\uc744 \ucc3e\uc744 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4. \ubaa8\ub378\uba85 \uc124\uc815\uc744 \ud655\uc778\ud574 \uc8fc\uc138\uc694.";
            case 429 -> "LLM \ud638\ucd9c \ud55c\ub3c4\ub97c \ucd08\uacfc\ud588\uc2b5\ub2c8\ub2e4. \uc7a0\uc2dc \ud6c4 \ub2e4\uc2dc \uc2dc\ub3c4\ud574 \uc8fc\uc138\uc694.";
            default -> "LLM \uc694\uccad\uc774 \uac70\ubd80\ub418\uc5c8\uc2b5\ub2c8\ub2e4. (HTTP " + status + ")";
        };
        return body(HttpStatus.BAD_GATEWAY, msg);
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<Map<String, String>> handleHttpServer(HttpServerErrorException ex) {
        log.error("LLM 5xx — status={} body={}", ex.getStatusCode().value(), safeBody(ex.getResponseBodyAsString()), ex);
        return body(HttpStatus.BAD_GATEWAY, "LLM \uc11c\ubc84\uc5d0\uc11c \uc77c\uc2dc\uc801\uc778 \uc624\ub958\uac00 \ubc1c\uc0dd\ud588\uc2b5\ub2c8\ub2e4. \uc7a0\uc2dc \ud6c4 \ub2e4\uc2dc \uc2dc\ub3c4\ud574 \uc8fc\uc138\uc694.");
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, String>> handleNetwork(ResourceAccessException ex) {
        log.error("LLM \ub124\ud2b8\uc6cc\ud06c/\ud0c0\uc784\uc544\uc6c3 — cause={}", ex.getClass().getSimpleName(), ex);
        return body(HttpStatus.BAD_GATEWAY, "LLM \uc11c\ubc84\uc5d0 \uc5f0\uacb0\ud560 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
    }

    @ExceptionHandler(NonTransientAiException.class)
    public ResponseEntity<Map<String, String>> handleAiNonTransient(NonTransientAiException ex) {
        log.error("Spring AI \uc601\uad6c \uc624\ub958 — {}", ex.getMessage(), ex);
        return body(HttpStatus.BAD_GATEWAY, "LLM \uc751\ub2f5 \ucc98\ub9ac\uc5d0\uc11c \uc601\uad6c\uc801\uc778 \uc624\ub958\uac00 \ubc1c\uc0dd\ud588\uc2b5\ub2c8\ub2e4.");
    }

    @ExceptionHandler(TransientAiException.class)
    public ResponseEntity<Map<String, String>> handleAiTransient(TransientAiException ex) {
        log.warn("Spring AI \uc77c\uc2dc \uc624\ub958 — {}", ex.getMessage(), ex);
        return body(HttpStatus.BAD_GATEWAY, "LLM \uc751\ub2f5\uc774 \uc77c\uc2dc\uc801\uc73c\ub85c \uc9c0\uc5f0\ub418\uace0 \uc788\uc2b5\ub2c8\ub2e4.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegal(IllegalArgumentException ex) {
        log.info("validation failed — {}", ex.getMessage());
        return body(HttpStatus.BAD_REQUEST, reasonOrDefault(ex.getMessage(), "\uc785\ub825 \uac12\uc774 \uc62c\ubc14\ub974\uc9c0 \uc54a\uc2b5\ub2c8\ub2e4."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception ex) {
        log.error("\ucc98\ub9ac\ub418\uc9c0 \uc54a\uc740 \uc608\uc678 — type={} msg={}", ex.getClass().getName(), ex.getMessage(), ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "\uc608\uc0c1\uce58 \ubabb\ud55c \uc624\ub958\uac00 \ubc1c\uc0dd\ud588\uc2b5\ub2c8\ub2e4.");
    }

    private static ResponseEntity<Map<String, String>> body(HttpStatusCode status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }

    private static String reasonOrDefault(String reason, String fallback) {
        return (reason == null || reason.isBlank()) ? fallback : reason;
    }

    private static String safeBody(String raw) {
        if (raw == null) return "";
        String stripped = raw.replaceAll("\\s+", " ").trim();
        return stripped.length() <= 500 ? stripped : stripped.substring(0, 497) + "...";
    }
}
