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

기존 compose가 참조하던 `bitnami/kafka:3.7` 태그를 더 이상 가져올 수 없었다.

## 판단 범위

확인된 사실은 배포 시점에 해당 태그 조회가 실패했다는 것이다. 벤더 정책 전체를 추정하기보다,
재현 가능한 이미지와 설정을 확보하는 데 초점을 맞췄다.

## 선택지
1. **다른 Bitnami 태그 사용** — 변경 범위는 작지만 같은 배포 정책에 계속 의존
2. **Apache 공식 이미지** — KRaft 단일 broker 데모에 필요한 기능을 제공하고 이미지 출처를 단순화
3. **Confluent Platform 이미지** — 추가 기능이 있지만 현재 데모 요구와 자원 제약에는 범위가 큼

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
외부 의존성은 API뿐 아니라 container image와 tag 정책에도 존재한다.
재현 가능한 배포를 위해 digest 또는 명시적 버전을 사용하고, 대체 이미지의 환경 변수·볼륨·health check 차이를 배포 전에 검증해야 한다.
