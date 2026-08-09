---
slug: kafka-dlt-observer-removal
unit: 5
title: DLT 관측용 Consumer 제거 — 측정 목적에 맞춘 자원 축소
date: 2026-05-24
tags: [kafka, refactoring, resource]
---

## 변경 전 구조

초기 구현은 주문 처리 consumer와 DLT 관측 consumer를 각각 `concurrency=3`으로 실행했다.

```text
orders consumer:       3 threads
DLT observer consumer: 3 threads
```

당시 DLT는 1 partition이었기 때문에 관측 consumer의 두 thread는 partition을 할당받지 못한 채 polling과 group 관리 비용만 발생했다. DLT 메시지를 재처리하지 않고 화면 카운터만 갱신하는 목적에 비해 별도 consumer group의 비용이 컸다.

현재는 원본 partition을 보존하는 `DeadLetterPublishingRecoverer` 동작에 맞춰 DLT도 3 partitions으로 변경했다. 따라서 “DLT가 1 partition”이라는 설명은 **초기 구조에만 해당**한다.

## 결정

단순 데모 카운터를 위해 운영하던 DLT observer를 제거하고, recovery handler가 DLT 발행을 처리한 시점에 애플리케이션 메모리 카운터를 갱신하도록 변경했다.

```java
DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template) {
    @Override
    public void accept(ConsumerRecord<?, ?> record, Consumer<?, ?> consumer, Exception ex) {
        super.accept(record, consumer, ex);
        metrics.recordDlt();
    }
};
```

변경 당시 consumer thread는 6개에서 3개로 줄었고, 별도 group의 polling·heartbeat·offset 관리가 제거됐다.

## 지표 의미의 한계

현재 `dlt` 카운터는 **recovery handler가 DLT 발행 절차를 수행한 횟수**다. 브로커에 레코드가 영구 저장됐음을 독립 consumer가 확인한 수치와 동일하다고 보장하지 않는다. 따라서 이 값은 데모 흐름 관찰에는 사용할 수 있지만, 운영 환경의 전달 완료·재처리 가능 건수를 증명하는 지표로 사용하면 안 된다.

운영 수준에서 DLT 도착을 보장하고 측정하려면 다음이 필요하다.

- producer send result와 실패를 분리해 기록
- 실제 DLT consumer 기반 전달 통합 테스트
- retry 횟수와 DLT header 검증
- replay 처리 상태와 업무 멱등성 관리

## 별도 DLT Consumer가 필요한 경우

- 실패 메시지를 수동 또는 자동 replay할 때
- 알림·DB 저장 등 후속 처리를 수행할 때
- DLT 처리를 별도 인스턴스와 장애 경계로 분리할 때

이번 제거는 DLT consumer가 불필요하다는 일반 결론이 아니라, **단순 화면 카운팅이라는 현재 요구에 비해 구조가 무거웠다**는 판단이다.
