---
slug: kafka-retry-concurrency
unit: 5
title: 블로킹 재시도와 파티션 병렬성 — concurrency 조정 근거
date: 2026-05-22
tags: [kafka, performance]
---

## 관찰

단일 consumer thread 구성에서 1,000건 처리에 약 90초가 걸렸고, 측정 처리량은 약 11.6 msg/s였다. 이 수치는 당시 로컬 데모 환경에서 관찰한 값이며 일반적인 Kafka 성능 기준은 아니다.

## 원인 분석

- 원본 토픽: 3 partitions
- consumer concurrency: 1
- 실패 조건: `orderId`가 17의 배수
- 오류 처리: `FixedBackOff(500ms, 3)`

`FixedBackOff(500ms, 3)`은 최초 처리 실패 후 최대 3회 재시도한다. 실패 레코드 하나가 해당 consumer thread를 최대 약 1.5초 점유할 수 있다. concurrency가 1이면 이 thread가 담당하는 다른 partition도 그 시간 동안 처리 기회를 얻지 못한다.

1,000건 중 실패 대상은 약 58건이므로, 재시도 대기만 단순 합산해도 최대 약 87초다. 실제 시간은 처리 비용과 스케줄링에 따라 달라지지만 관찰값과 같은 범위였다.

## 결정

현재 토픽의 partition 수에 맞춰 consumer concurrency를 3으로 설정했다.

```properties
spring.kafka.listener.concurrency=3
```

각 partition을 별도 consumer thread가 담당할 수 있어 한 partition의 블로킹 재시도가 다른 partition까지 정지시키는 범위를 줄였다. 당시 동일 조건에서 1,000건 처리 시간이 약 30초로 감소했다.

## 적용 범위와 한계

`concurrency = partition 수`는 이 데모의 병렬성 상한에 맞춘 값이지 모든 시스템의 고정 공식은 아니다. 실제 운영에서는 다음을 함께 본다.

- consumer instance 수와 partition 배치
- 레코드별 처리 비용 및 순서 보장 범위
- downstream 시스템의 처리 용량
- rebalance 비용과 CPU·메모리 사용량
- 블로킹 재시도 대신 retry topic을 사용할지 여부

처리량을 높이는 것만이 목표라면 concurrency 증가는 증상 완화에 가깝다. 실패가 길게 지속되는 환경에서는 main topic 처리를 점유하지 않는 non-blocking retry 구조를 별도로 검토해야 한다.
