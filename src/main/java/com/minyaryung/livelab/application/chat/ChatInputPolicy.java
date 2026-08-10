package com.minyaryung.livelab.application.chat;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class ChatInputPolicy {

    private static final String PROMPT_ATTACK_MESSAGE =
            "지시 변경이나 내부 프롬프트 공개 요청은 처리하지 않습니다. 경력과 기술 경험을 질문해 주세요.";
    private static final String SENSITIVE_TOPIC_MESSAGE =
            "본업 외 정치·종교 관련 의견은 답변드리지 않습니다. 경력과 기술 경험을 질문해 주세요.";

    private static final Pattern PROMPT_ATTACK = Pattern.compile(String.join("|",
            "(?:이전|앞선|기존|위의?)\\s*(?:지시|규칙|명령).{0,20}(?:무시|잊어|폐기)",
            "(?:규칙|지시|명령).{0,20}(?:무시|우회)",
            "(?:시스템\\s*프롬프트|내부\\s*(?:지시|규칙)).{0,30}(?:보여|출력|공개|노출|말해)",
            "(?:없는|허위|가짜).{0,20}(?:경력|경험).{0,20}(?:만들|지어|꾸며|추측)",
            "(?:그냥\\s*)?추측.{0,20}(?:경력|경험).{0,20}(?:답|말)",
            "ignore.{0,20}(?:previous|prior|above).{0,20}(?:instruction|rule)",
            "(?:show|reveal|print|expose).{0,30}(?:system\\s*prompt|hidden\\s*instruction)",
            "(?:invent|fabricate|make\\s*up).{0,30}(?:career|experience)"),
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.DOTALL);

    private static final Pattern SENSITIVE_OPINION = Pattern.compile(
            "(?:(?:정치|종교|선거|대통령|정당).{0,20}(?:의견|입장|지지|평가)|"
                    + "(?:의견|입장|지지|평가).{0,20}(?:정치|종교|선거|대통령|정당)|"
                    + "(?:politic|religion|election|president|party).{0,20}(?:opinion|support|view))",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.DOTALL);

    public Optional<Rejection> evaluate(String question) {
        String normalized = question == null ? "" : question.strip().toLowerCase(Locale.ROOT);
        if (PROMPT_ATTACK.matcher(normalized).find()) {
            return Optional.of(new Rejection("prompt_attack", PROMPT_ATTACK_MESSAGE));
        }
        if (SENSITIVE_OPINION.matcher(normalized).find()) {
            return Optional.of(new Rejection("sensitive_topic", SENSITIVE_TOPIC_MESSAGE));
        }
        return Optional.empty();
    }

    public record Rejection(String reason, String message) {}
}
