package com.minyaryung.livelab.chat;

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

    // 의도적으로 던진 4xx — 사용자 입력 검증, 레이트리밋 등
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleStatus(ResponseStatusException ex) {
        log.info("client error {} — {}", ex.getStatusCode().value(), ex.getReason());
        return body(ex.getStatusCode(), reasonOrDefault(ex.getReason(), "요청 처리 실패"));
    }

    // 정적 리소스 404 — 브라우저의 favicon 자동 요청 등 정상 흐름이므로 ERROR 로깅 금지.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> handleNoResource(NoResourceFoundException ex) {
        log.debug("static resource not found: {}", ex.getResourcePath());
        return body(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다.");
    }

    // LLM 호출의 4xx (Gemini 측 인증·모델·쿼터)
    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<Map<String, String>> handleHttpClient(HttpClientErrorException ex) {
        int status = ex.getStatusCode().value();
        log.error("LLM 4xx — status={} body={}", status, safeBody(ex.getResponseBodyAsString()), ex);
        String msg = switch (status) {
            case 400 -> "LLM 요청 형식 오류입니다. 입력을 짧게 줄여 다시 시도해 주세요.";
            case 401, 403 -> "LLM 인증에 실패했습니다. 서버의 API 키 설정을 확인해 주세요.";
            case 404 -> "LLM 모델을 찾을 수 없습니다. 모델명 설정을 확인해 주세요.";
            case 429 -> "LLM 호출 한도를 초과했습니다. 잠시 후 다시 시도해 주세요.";
            default -> "LLM 요청이 거부되었습니다. (HTTP " + status + ")";
        };
        return body(HttpStatus.BAD_GATEWAY, msg);
    }

    // LLM 호출의 5xx (Gemini 측 일시 장애)
    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<Map<String, String>> handleHttpServer(HttpServerErrorException ex) {
        log.error("LLM 5xx — status={} body={}", ex.getStatusCode().value(), safeBody(ex.getResponseBodyAsString()), ex);
        return body(HttpStatus.BAD_GATEWAY, "LLM 서버에서 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
    }

    // 네트워크·타임아웃
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, String>> handleNetwork(ResourceAccessException ex) {
        log.error("LLM 네트워크/타임아웃 — cause={}", ex.getClass().getSimpleName(), ex);
        return body(HttpStatus.BAD_GATEWAY, "LLM 서버에 연결할 수 없습니다. 네트워크 상태를 확인하거나 잠시 후 다시 시도해 주세요.");
    }

    // Spring AI 영구 오류 (재시도해도 안 되는 류)
    @ExceptionHandler(NonTransientAiException.class)
    public ResponseEntity<Map<String, String>> handleAiNonTransient(NonTransientAiException ex) {
        log.error("Spring AI 영구 오류 — {}", ex.getMessage(), ex);
        return body(HttpStatus.BAD_GATEWAY, "LLM 응답 처리에서 영구적인 오류가 발생했습니다. 설정을 점검해 주세요.");
    }

    // Spring AI 일시 오류 (재시도 후 해결 가능성 있음)
    @ExceptionHandler(TransientAiException.class)
    public ResponseEntity<Map<String, String>> handleAiTransient(TransientAiException ex) {
        log.warn("Spring AI 일시 오류 — {}", ex.getMessage(), ex);
        return body(HttpStatus.BAD_GATEWAY, "LLM 응답이 일시적으로 지연되고 있습니다. 잠시 후 다시 시도해 주세요.");
    }

    // 입력 검증 누락분 (서비스에서 IllegalArgumentException 던지는 경우)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegal(IllegalArgumentException ex) {
        log.info("validation failed — {}", ex.getMessage());
        return body(HttpStatus.BAD_REQUEST, reasonOrDefault(ex.getMessage(), "입력 값이 올바르지 않습니다."));
    }

    // 최종 catch-all — 위에서 잡히지 않은 모든 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception ex) {
        log.error("처리되지 않은 예외 — type={} msg={}", ex.getClass().getName(), ex.getMessage(), ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "예상치 못한 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
    }

    private static ResponseEntity<Map<String, String>> body(HttpStatusCode status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }

    private static String reasonOrDefault(String reason, String fallback) {
        return (reason == null || reason.isBlank()) ? fallback : reason;
    }

    // LLM 응답 본문은 길거나 키 정보가 섞일 수 있어 로그 안전 길이로 자른다.
    private static String safeBody(String raw) {
        if (raw == null) return "";
        String stripped = raw.replaceAll("\\s+", " ").trim();
        return stripped.length() <= 500 ? stripped : stripped.substring(0, 497) + "...";
    }
}
