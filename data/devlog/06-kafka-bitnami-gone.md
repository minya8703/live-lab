---
slug: kafka-bitnami-vanished
unit: 5
title: 벤더가 이미지를 내렸을 때 — Bitnami → Apache 공식으로 마이그레이션
date: 2026-05-22
tags: [docker, kafka, vendor-lock]
---

## 증상
`docker compose up -d kafka` 가 실패:

```
✘ kafka Error failed to resolve reference "docker.io/bitnami/kafka:3.7":
docker.io/bitnami/kafka:3.7: not found
```

`bitnami/kafka:3.7` 는 흔히 쓰던 태그였는데 사라졌다.

## 배경
Bitnami는 2025년부터 일부 태그를 **유료 "Bitnami Secure Images"** 로 분리,
무료 배포에서 점진적으로 제거하는 정책을 시행했다.
이걸 모르고 `bitnami/kafka:3.7` 를 그대로 쓰던 수많은 docker-compose 가 갑자기 깨졌다.

## 선택지
1. **무료로 남은 Bitnami 태그** (예: `latest`, `3.7.0`) — 임시방편. 다음에 또 사라질 수 있음.
2. **Apache 공식 이미지** — Confluent 없는 순수 Kafka. KRaft 모드를 1급 시민으로 지원.
3. **Confluent Platform 이미지** — 더 안정적이지만 Kafka 만 쓰기엔 무거움.

채택: **Apache 공식** (`apache/kafka:3.7.0`).

## 마이그레이션에서 마주친 차이
Bitnami → Apache 환경 변수 네이밍 차이:

| Bitnami | Apache 공식 |
|---|---|
| `KAFKA_CFG_NODE_ID` | `KAFKA_NODE_ID` |
| `KAFKA_CFG_PROCESS_ROLES` | `KAFKA_PROCESS_ROLES` |
| `ALLOW_PLAINTEXT_LISTENER` | (불필요) |
| `KAFKA_CFG_LOG_DIRS` | `KAFKA_LOG_DIRS` |
| (없음) | `CLUSTER_ID` (필수) |

KRaft 모드는 첫 부팅 시 클러스터에 고정 UUID(`CLUSTER_ID`) 가 필요하다 — 데이터 디렉토리 포맷에 사용.

## 교훈
**벤더 락인은 의외의 곳에서 터진다.** 이번엔 코드도 라이선스도 아니고 *Docker 이미지 태그*였다.

이 사건 자체가 면접에서 답하기 좋은 종류다 —
*"외부 의존성이 사라졌을 때 빠르게 대안을 찾고 마이그레이션할 수 있는가"* 라는 질문에
구체적 사례로 답할 수 있다.
