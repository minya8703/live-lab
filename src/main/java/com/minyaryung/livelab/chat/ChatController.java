package com.minyaryung.livelab.chat;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final int MAX_LENGTH = 500;

    private final ChatService chatService;
    private final SimpleRateLimiter rateLimiter;

    public ChatController(ChatService chatService, SimpleRateLimiter rateLimiter) {
        this.chatService = chatService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request, HttpServletRequest http) {
        String message = request == null ? null : request.message();
        if (message == null || message.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "질문이 비어 있습니다.");
        }
        String trimmed = message.strip();
        if (trimmed.length() > MAX_LENGTH) {
            throw new ResponseStatusException(BAD_REQUEST,
                "질문은 " + MAX_LENGTH + "자 이내로 부탁드립니다.");
        }
        if (!rateLimiter.tryAcquire(clientKey(http))) {
            throw new ResponseStatusException(TOO_MANY_REQUESTS,
                "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
        }
        String answer = chatService.ask(trimmed);
        return new ChatResponse(answer);
    }

    private String clientKey(HttpServletRequest http) {
        String forwarded = http.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return comma == -1 ? forwarded.strip() : forwarded.substring(0, comma).strip();
        }
        return http.getRemoteAddr();
    }

    public record ChatRequest(String message) {}
    public record ChatResponse(String answer) {}
}
