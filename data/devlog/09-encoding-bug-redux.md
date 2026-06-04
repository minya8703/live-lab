---
slug: encoding-bug-redux
unit: 7
title: 교훈을 적고도 다시 같은 실수 — status 라벨 mojibake
date: 2026-05-23
tags: [encoding, meta-retrospective]
---

## 증상
랜딩 페이지 상단 상태 배지에 이렇게 표시:

```
AI-DLC ê°ë° ì¼ì§ íì´ì§ êµ¬ì¶ ì¤ Â· AI-DLC Construction
```

명백히 mojibake. `Â·` 는 UTF-8 의 `·`(C2 B7) 가 Latin-1 로 해석된 흔적,
`ê°ë°` 는 "개발"(EA B0 9C EB B0 9C)이 Latin-1 로 해석되며 9C 가 invisible 처리된 결과.

## 메타 회고
바로 직전에 작성한 [05-encoding-bug.md](#properties-encoding-bug) 일지의 룰:

> 한국어/일본어 같은 비-ASCII 정적 데이터는 properties 가 아니라 Java 코드에 둔다.

그런데 정작 같은 시점에 status 라벨은 `application.properties` 에 그대로 박아두고 있었다:

```properties
livelab.status.label=AI-DLC 개발 일지 페이지 구축 중 · AI-DLC Construction
```

Spring Boot 가 properties 를 (특정 조건에서) ISO-8859-1 로 읽어 메모리에 Korean 이 손상된 채 저장,
Jackson 이 손상된 문자열을 다시 UTF-8 로 인코딩 → 이중 mojibake.

## 해결
같은 룰을 status 에도 적용 — `UnitProgress.java` 에 Java 상수로 이동:

```java
static final int CURRENT_UNIT = 7;
static final int TOTAL_UNITS = 10;
static final String CURRENT_LABEL = "AI-DLC 개발 일지 페이지 구축 중 · AI-DLC Construction";
```

추가 안전장치로 응답 Content-Type 에 charset 명시:

```java
@RequestMapping(
    value = "/api/status",
    produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
```

## 진짜 교훈
**룰을 적는 것과 룰을 적용하는 것은 다른 작업이다.**

회고를 적었어도 *그 룰을 모든 기존 코드에 소급 적용하는 한 사이클* 이 빠지면
같은 버그가 반복된다.

다음에 새 회고를 적을 때는 항상 두 가지를 같이 한다:
1. 룰을 글로 적는다 (= 일지)
2. 룰을 위반하는 기존 코드를 한 번 훑어 같이 고친다 (= 마이그레이션)

이 사건 자체가 "리팩토링 챕터" 의 정당성을 증명한다 —
새 규칙은 즉시 적용되어야 살아남는다.
