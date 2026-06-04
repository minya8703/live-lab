package com.minyaryung.livelab.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient chatClient;

    public ChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String ask(String question) {
        long started = System.currentTimeMillis();
        String answer = chatClient.prompt().user(question).call().content();
        long elapsed = System.currentTimeMillis() - started;
        log.info("chat answered in {}ms — q='{}' (len {})", elapsed, abbreviate(question), question.length());
        return answer;
    }

    private static String abbreviate(String s) {
        if (s == null) return "";
        return s.length() <= 80 ? s : s.substring(0, 77) + "...";
    }
}
