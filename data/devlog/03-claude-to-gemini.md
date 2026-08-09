---
slug: claude-to-gemini-swap
unit: 3
title: Spring AI 경계 검증 — 애플리케이션 코드 변경 없이 Claude → Gemini
date: 2026-05-21
tags: [spring-ai, abstraction, cost]
---

## 맥락
챗봇 V0은 Anthropic Claude로 만들었다. 동작이 확인된 직후 비용을 재검토하니
당시 요금과 호출량을 기준으로 Google Gemini Flash가 비용 가드레일에 더 적합했다.

검증 대상은 provider 변경이 애플리케이션 계층까지 전파되는지였다.

## Spring AI 의 핵심 가치 검증

결과: **애플리케이션 Java 코드 변경 없음**. 서비스와 컨트롤러의 계약은 유지했다.

변경은 두 곳뿐:
1. `build.gradle`: Anthropic starter → OpenAI starter
2. `application.properties`: 4줄 (api-key, base-url, completions-path, model)

```properties
spring.ai.openai.api-key=${GOOGLE_API_KEY:}
spring.ai.openai.base-url=https://generativelanguage.googleapis.com
spring.ai.openai.chat.completions-path=/v1beta/openai/chat/completions
spring.ai.openai.chat.options.model=gemini-2.5-flash
```

Google AI Studio의 OpenAI 호환 엔드포인트를 Spring AI의 OpenAI client로 호출했다.
애플리케이션 계층이 provider SDK에 직접 의존하지 않았기 때문에 변경 범위를 설정과 의존성 정의에 제한할 수 있었다.

## 한계

provider별 응답 형식, tool calling, 안전 정책까지 모두 호환된다는 뜻은 아니다.
이번 검증 범위는 단순 chat completion 계약이었으며 provider 고유 기능을 사용하면 adapter나 회귀 테스트가 추가로 필요하다.

## 교훈
추상화의 가치는 도입 자체가 아니라 변경 시 영향을 경계 안에 제한했는지로 검증해야 한다.
