---
slug: claude-to-gemini-swap
unit: 3
title: Spring AI 추상화의 진짜 가치 — Java 코드 0줄 변경으로 Claude → Gemini
date: 2026-05-21
tags: [spring-ai, abstraction, cost]
---

## 맥락
챗봇 V0은 Anthropic Claude로 만들었다. 동작이 확인된 직후 비용을 재검토하니
Google Gemini Flash 무료 티어가 이 트래픽 규모에 충분하다는 게 보였다.
"월 비용 $15" 가드레일에 더 여유가 생긴다.

문제는 — 이미 만들어둔 코드를 얼마나 갈아엎어야 하나?

## Spring AI 의 핵심 가치 검증

결과: **Java 코드 0줄**. `ChatService`/`ChatController`/`ChatClientConfig` 모두 그대로.

변경은 두 곳뿐:
1. `pom.xml`: `spring-ai-starter-model-anthropic` → `spring-ai-starter-model-openai`
2. `application.properties`: 4줄 (api-key, base-url, completions-path, model)

```properties
spring.ai.openai.api-key=${GOOGLE_API_KEY:}
spring.ai.openai.base-url=https://generativelanguage.googleapis.com
spring.ai.openai.chat.completions-path=/v1beta/openai/chat/completions
spring.ai.openai.chat.options.model=gemini-2.5-flash
```

Google AI Studio가 **OpenAI 호환 엔드포인트**를 노출하므로 Spring AI 의 OpenAI 클라이언트가
그대로 통한다 — 어떤 의미에서 이중 추상화의 이득.

## 면접용 요약
"Spring AI 도입의 진짜 가치가 뭐냐"는 질문에 이 사례로 답할 수 있다.
프레임워크는 *지금* 편리해서 쓰는 게 아니라 *나중에* 갈아탈 때 자유로워서 쓰는 거다.

## 교훈
프로바이더 락인을 늦게 후회하지 않는 가장 싼 보험은 — 처음부터 추상화 위에서 시작하는 것.
