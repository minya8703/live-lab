---
slug: kafka-retry-concurrency
unit: 5
title: 단일 컨슈머 스레드가 재시도에 묶일 때 — concurrency=파티션수
date: 2026-05-22
tags: [kafka, performance]
---

## 증상
Kafka 데모 처리량이 **11.6 msgs/sec**. 너무 느림.
1000건 발행에 90초 가까이 걸린다.

## 원인 분석
- 토픽 파티션 3개
- Spring Kafka 기본 `concurrency=1` (컨슈머 스레드 1개)
- 17의 배수 메시지는 의도적 실패 → `FixedBackOff(500ms, 3)` 재시도
- 단일 스레드가 재시도 대기 중이면 다른 파티션 처리도 **함께 멈춤**

계산:
- 1000건 중 17의 배수 ≈ 58건 실패
- 각 실패 메시지: 1 시도 + 2 재시도 × 500ms = 1초+
- 58 × 1초 = ~60초 재시도 대기
- 정상 메시지 처리는 그 사이 블로킹

## 해결
파티션 수 만큼 컨슈머 스레드를 늘림:

```properties
spring.kafka.listener.concurrency=3
```

3 파티션 × 3 스레드 = 한 스레드의 재시도가 다른 파티션 처리를 막지 않음.
처리량 3배 ↑ — 1000건이 30초 안에 끝.

## 면접용 통찰
> "Kafka 의 ConcurrentKafkaListenerContainerFactory에서 concurrency 를 파티션 수로 맞추는 게 일반론이지만,
> 진짜 이유는 *retry backoff 가 다른 파티션을 block 하지 않게* 하기 위해서입니다.
> 단일 컨슈머는 throughput 손실의 가장 흔한 원인입니다."

## 교훈
**처리량 문제는 백오프 정책과 분리해서 보면 안 된다.**
"throughput이 낮다" → "concurrency 늘리자"는 깊이 없음.
"재시도 정책이 단일 스레드를 잡아먹고 있다" → "파티션 만큼 병렬화"가 깊이 있는 답.
