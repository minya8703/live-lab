package com.minyaryung.livelab.presentation.chat;

import com.minyaryung.livelab.application.chat.ChatAnswer;
import com.minyaryung.livelab.application.chat.ChatService;
import com.minyaryung.livelab.infra.common.SimpleRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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
        if (message == null || message.isBlank())
            throw new ResponseStatusException(BAD_REQUEST, "\uc9c8\ubb38\uc774 \ube44\uc5b4 \uc788\uc2b5\ub2c8\ub2e4.");
        String trimmed = message.strip();
        if (trimmed.length() > MAX_LENGTH)
            throw new ResponseStatusException(BAD_REQUEST, "\uc9c8\ubb38\uc740 " + MAX_LENGTH + "\uc790 \uc774\ub0b4\ub85c \ubd80\ud0c1\ub4dc\ub9bd\ub2c8\ub2e4.");
        if (!rateLimiter.tryAcquire(clientKey(http)))
            throw new ResponseStatusException(TOO_MANY_REQUESTS, "\uc694\uccad\uc774 \ub108\ubb34 \ub9ce\uc2b5\ub2c8\ub2e4. \uc7a0\uc2dc \ud6c4 \ub2e4\uc2dc \uc2dc\ub3c4\ud574 \uc8fc\uc138\uc694.");
        ChatAnswer result = chatService.ask(trimmed);
        return new ChatResponse(result.answer(), result.sources(), result.grounded());
    }

    private String clientKey(HttpServletRequest http) {
        // Forwarded headers are caller-controlled unless the direct peer is a verified proxy.
        // Secure default: use the TCP peer address and accept shared limits behind Cloudflare.
        return http.getRemoteAddr();
    }

    public record ChatRequest(String message) {}
    public record ChatResponse(String answer, List<String> sources, boolean grounded) {}
}
