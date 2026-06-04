---
slug: kafka-dlt-observer-removal
unit: 5
title: DLT 카운팅을 위해 별도 컨슈머를 띄우는 건 과한 설계였다
date: 2026-05-24
tags: [kafka, refactoring, resource]
---

## 증상
앱 종료 시 로그가 시끄러웠다 — 컨슈머 스레드 6개가 graceful shutdown 됨:

```
livelab-orders-consumer: Consumer stopped  (× 3)
livelab-orders-dlt-observer: Consumer stopped  (× 3)
```

각 스레드마다 `JmxReporter` + `ClientTelemetryReporter` 정리 로그까지 곱하면 셧다운 한 번에
수십 줄의 비슷한 로그가 쏟아진다. 운영 시점엔 더 시끄러울 것.

## 원인
**`@KafkaListener` 가 2개** (orders + DLT observer)
× **concurrency=3** = 6 컨슈머 스레드.

```
| Container | Group                          | Partitions | Threads | Utilization |
| #1 orders | livelab-orders-consumer        | 3          | 3       | 100%        |
| #0 DLT    | livelab-orders-dlt-observer    | 1          | 3       | 33%         |
```

DLT 토픽은 파티션이 1개라 3 스레드 중 2개는 *영원히 idle* 한 상태로 폴링만 한다.
**1 파티션 토픽에 3 컨슈머를 붙이는 건 정의상 낭비**.

게다가 DLT observer 가 하는 일은 단순히 *"DLT에 메시지 1건 들어왔다"* 라는 카운터 +1 뿐이었다.
그걸 위해 별도 컨슈머 그룹을 띄우고, 별도 offset 을 관리하고, 별도 폴링 루프를 도는 건
구조적으로 과했다.

## 해결
DLT 카운트를 메시지가 DLT로 발행되는 *바로 그 순간* — 즉 `DeadLetterPublishingRecoverer.accept()` —
에서 메모리 카운터 +1 하도록 옮겼다.

```java
DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template) {
    @Override
    public void accept(ConsumerRecord<?, ?> record, Exception ex) {
        super.accept(record, ex);
        metrics.recordDlt();
    }
};
```

DLT observer `@KafkaListener` 제거. 결과:
- 컨슈머 스레드 6개 → 3개 (절반)
- 폴링/heartbeat 부하 절반
- 셧다운 로그도 절반

## 추가 정리
Kafka 3.7+ 클라이언트가 추가한 `ClientTelemetry` 가 5분마다 브로커로 메트릭 푸시하면서
로그 노이즈를 키운다. 데모/로컬 환경에서는 필요 없으니 끈다:

```properties
spring.kafka.consumer.properties.enable.metrics.push=false
spring.kafka.producer.properties.enable.metrics.push=false
logging.level.org.apache.kafka=WARN
```

## 교훈
**"메시지가 토픽에 들어왔다는 사실을 알기 위해 컨슈머 그룹을 띄울 필요가 없다."**
대부분의 경우 *발행 시점의 callback / interceptor* 로 동일 정보를 얻을 수 있고,
그게 자원도 덜 쓴다.

별도 컨슈머 그룹이 정당한 경우:
- DLT 메시지를 *재처리* 해야 할 때 (수동 또는 자동 replay)
- DLT 메시지를 *외부 시스템*(예: Slack 알림, 별도 DB)으로 흘릴 때
- DLT 처리를 *다른 인스턴스*에서 격리할 때

단순 카운팅·로깅 용도면 별도 컨슈머는 사치다.
