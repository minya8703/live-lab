package com.minyaryung.livelab.presentation.chat;

import com.minyaryung.livelab.application.chat.ChatService;
import com.minyaryung.livelab.infra.common.SimpleRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatControllerTest {

    @Test
    void ignoresCallerSuppliedForwardedForWhenRateLimiting() {
        ChatService chatService = mock(ChatService.class);
        SimpleRateLimiter rateLimiter = mock(SimpleRateLimiter.class);
        when(rateLimiter.tryAcquire("203.0.113.10")).thenReturn(true);
        when(chatService.ask("경력을 알려주세요")).thenReturn("답변");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("X-Forwarded-For", "198.51.100.25");

        ChatController controller = new ChatController(chatService, rateLimiter);
        ChatController.ChatResponse response = controller.chat(
                new ChatController.ChatRequest("경력을 알려주세요"), request);

        assertThat(response.answer()).isEqualTo("답변");
        verify(rateLimiter).tryAcquire("203.0.113.10");
    }
}
