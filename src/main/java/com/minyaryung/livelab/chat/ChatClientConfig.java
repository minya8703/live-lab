package com.minyaryung.livelab.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    private static final Logger log = LoggerFactory.getLogger(ChatClientConfig.class);

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, SystemPromptBuilder promptBuilder) {
        String systemPrompt = promptBuilder.build();
        log.info("system prompt built — {} chars", systemPrompt.length());
        return builder.defaultSystem(systemPrompt).build();
    }
}
