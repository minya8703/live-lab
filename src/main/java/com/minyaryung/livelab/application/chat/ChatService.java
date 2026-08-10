package com.minyaryung.livelab.application.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minyaryung.livelab.application.career.CareerDataLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final int MAX_ANSWER_LENGTH = 4_000;
    private static final String UNGROUNDED_MESSAGE =
            "경력 데이터에서 검증 가능한 근거를 확인하지 못해 답변을 보류합니다.";
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final Set<String> validSourceIds;
    private final ChatInputPolicy inputPolicy;

    public ChatService(ChatClient chatClient, ObjectMapper objectMapper,
                       CareerDataLoader careerDataLoader, ChatInputPolicy inputPolicy) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
        this.validSourceIds = Set.copyOf(careerDataLoader.sourceIds());
        this.inputPolicy = inputPolicy;
    }

    public ChatAnswer ask(String question) {
        Optional<ChatInputPolicy.Rejection> rejection = inputPolicy.evaluate(question);
        if (rejection.isPresent()) {
            log.info("chat rejected by input policy reason={}", rejection.get().reason());
            return new ChatAnswer(rejection.get().message(), List.of(), false);
        }

        long started = System.currentTimeMillis();
        String rawResponse = chatClient.prompt().user(question).call().content();
        ChatAnswer answer = validateResponse(rawResponse);
        long elapsed = System.currentTimeMillis() - started;
        log.info("chat answered — elapsedMs={} questionLength={} grounded={} sourceCount={}",
                elapsed, question.length(), answer.grounded(), answer.sources().size());
        return answer;
    }

    ChatAnswer validateResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            log.warn("chat response rejected reason=empty");
            return ungrounded();
        }

        try {
            ChatAnswer candidate = objectMapper.readValue(extractJson(rawResponse), ChatAnswer.class);
            if (candidate.answer() == null || candidate.answer().isBlank()
                    || candidate.answer().length() > MAX_ANSWER_LENGTH) {
                log.warn("chat response rejected reason=invalid_answer");
                return ungrounded();
            }
            if (containsInternalPromptMaterial(candidate.answer())) {
                log.warn("chat response rejected reason=prompt_material");
                return ungrounded();
            }

            List<String> sources = normalizeSources(candidate.sources());
            if (!candidate.grounded()) {
                return ungrounded();
            }

            if (sources.isEmpty() || !validSourceIds.containsAll(sources)) {
                log.warn("chat response rejected reason=invalid_sources sourceCount={}", sources.size());
                return ungrounded();
            }
            return new ChatAnswer(candidate.answer().strip(), sources, true);
        } catch (JsonProcessingException ex) {
            log.warn("chat response rejected reason=invalid_json type={}", ex.getClass().getSimpleName());
            return ungrounded();
        }
    }

    private static List<String> normalizeSources(List<String> sources) {
        if (sources == null) return List.of();
        return sources.stream()
                .filter(source -> source != null && !source.isBlank())
                .map(String::strip)
                .distinct()
                .limit(3)
                .toList();
    }

    private static String extractJson(String rawResponse) {
        String value = rawResponse.strip();
        if (value.startsWith("```")) {
            int firstNewline = value.indexOf('\n');
            int closingFence = value.lastIndexOf("```");
            if (firstNewline >= 0 && closingFence > firstNewline) {
                return value.substring(firstNewline + 1, closingFence).strip();
            }
        }
        return value;
    }

    private static boolean containsInternalPromptMaterial(String answer) {
        String normalized = answer.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("===== file:")
                || normalized.contains("[경력 데이터]")
                || normalized.contains("엄격한 응답 규칙")
                || normalized.contains("당신은 민야령의 백엔드 경력 q&a 챗봇입니다");
    }

    private static ChatAnswer ungrounded() {
        return new ChatAnswer(UNGROUNDED_MESSAGE, List.of(), false);
    }
}
