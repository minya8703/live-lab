package com.minyaryung.livelab.application.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minyaryung.livelab.application.career.CareerDataLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final int MAX_ANSWER_LENGTH = 4_000;
    private static final int MIN_EVIDENCE_QUOTE_LENGTH = 8;
    private static final int MAX_EVIDENCE_QUOTE_LENGTH = 500;
    private static final String UNGROUNDED_MESSAGE =
            "경력 데이터에서 검증 가능한 근거를 확인하지 못해 답변을 보류합니다.";
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final Map<String, String> sourceDocuments;
    private final ChatInputPolicy inputPolicy;

    public ChatService(ChatClient chatClient, ObjectMapper objectMapper,
                       CareerDataLoader careerDataLoader, ChatInputPolicy inputPolicy) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
        this.sourceDocuments = careerDataLoader.documentsBySourceId().entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> normalizeWhitespace(entry.getValue())));
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
            ModelResponse candidate = objectMapper.readValue(extractJson(rawResponse), ModelResponse.class);
            if (candidate.answer() == null || candidate.answer().isBlank()
                    || candidate.answer().length() > MAX_ANSWER_LENGTH) {
                log.warn("chat response rejected reason=invalid_answer");
                return ungrounded();
            }
            if (containsInternalPromptMaterial(candidate.answer())) {
                log.warn("chat response rejected reason=prompt_material");
                return ungrounded();
            }

            if (!candidate.grounded()) {
                return ungrounded();
            }

            Optional<List<String>> validatedSources = validateEvidence(candidate.evidence());
            if (validatedSources.isEmpty()) {
                log.warn("chat response rejected reason=invalid_evidence");
                return ungrounded();
            }
            return new ChatAnswer(candidate.answer().strip(), validatedSources.get(), true);
        } catch (JsonProcessingException ex) {
            log.warn("chat response rejected reason=invalid_json type={}", ex.getClass().getSimpleName());
            return ungrounded();
        }
    }

    private Optional<List<String>> validateEvidence(List<Evidence> evidence) {
        if (evidence == null || evidence.isEmpty() || evidence.size() > 3) {
            return Optional.empty();
        }

        for (Evidence item : evidence) {
            if (item == null || item.source() == null || item.quote() == null) {
                return Optional.empty();
            }
            String source = item.source().strip();
            String quote = normalizeWhitespace(item.quote());
            String document = sourceDocuments.get(source);
            if (document == null
                    || quote.length() < MIN_EVIDENCE_QUOTE_LENGTH
                    || quote.length() > MAX_EVIDENCE_QUOTE_LENGTH
                    || !document.contains(quote)) {
                return Optional.empty();
            }
        }

        return Optional.of(evidence.stream()
                .map(item -> item.source().strip())
                .distinct()
                .toList());
    }

    private static String normalizeWhitespace(String value) {
        return value.strip().replaceAll("\\s+", " ");
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
        String normalized = answer.toLowerCase(Locale.ROOT);
        return normalized.contains("===== file:")
                || normalized.contains("[경력 데이터]")
                || normalized.contains("엄격한 응답 규칙")
                || normalized.contains("당신은 민야령의 백엔드 경력 q&a 챗봇입니다");
    }

    private static ChatAnswer ungrounded() {
        return new ChatAnswer(UNGROUNDED_MESSAGE, List.of(), false);
    }

    private record ModelResponse(String answer, List<Evidence> evidence, boolean grounded) {}

    private record Evidence(String source, String quote) {}
}
