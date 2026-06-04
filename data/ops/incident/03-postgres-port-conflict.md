---
slug: postgres-port-conflict
title: "auth failed" 가 인증 문제가 아닐 때 — 호스트의 다른 Postgres
date: 2026-05-22
tags: [debugging, docker, postgres]
---

## 증상

Spring Boot 기동 시:
```
PSQLException: 치명적오류: 사용자 "livelab"의 password 인증을 실패했습니다
```

`.env` 의 비번·`docker-compose.yml` 의 비번·컨테이너 내부의 비번 모두 같은 값. 그런데도 Spring 만 인증 실패.

## 의심한 가설 (시간순)

1. `.env` 인코딩 (UTF-16 BOM?) — UTF-8 확인
2. `spring-dotenv` 가 `.env` 못 읽음 — `GOOGLE_API_KEY` 는 잘 읽음
3. `POSTGRES_PASSWORD` env 의 default placeholder (`livelab`) 우선순위 — placeholder 빼고 직박해도 동일
4. PowerShell 세션 env 가 덮어씀 — `Remove-Item env:POSTGRES_PASSWORD` 후에도 동일

## 결정적 단서

```bash
# 컨테이너 안에서: 성공 (PGPASSWORD env 로 비번 주입)
docker compose exec postgres psql -h localhost -U livelab -d livelab -c "select 1"
# → 1
```

```bash
# 호스트에서: 실패
PowerShell> psql -h localhost -U livelab -d livelab
# → password authentication failed
```

**같은 비번, 같은 사용자명, 다른 결과.** 이 비대칭이 단 하나의 시나리오를 가리킴 — *"두 환경이 서로 다른 Postgres 에 붙고 있다"*.

## 진짜 원인

`docker volume ls` 에 다른 프로젝트 흔적이 보였음 (`raonnanal-api_postgres_data`). 사용자 PC 에 다른 Postgres 가 5432 포트를 점유 중. Spring 의 `localhost:5432` 가 우리 Docker 컨테이너가 아니라 그 다른 Postgres 로 가서, 거기엔 `livelab` 사용자가 없으니 거부.

## 해결

호스트 포트를 5433 으로 옮겨 격리:

```yaml
# docker-compose.yml
ports:
  - "${POSTGRES_HOST_PORT:-5433}:5432"
```

```properties
# application.properties
spring.datasource.url=jdbc:postgresql://localhost:${POSTGRES_HOST_PORT:5433}/livelab
```

## 일반화한 룰

**로컬 개발에서 "auth failed" 또는 "connection refused" 를 봤을 때 자격증명 디버깅 전에 포트 충돌부터 점검.**

진단 패턴:
1. 호스트와 컨테이너 양쪽에서 같은 클라이언트로 같은 인증 시도
2. 결과가 다르면 99% 다른 서비스에 연결 중
3. `netstat -ano | findstr :<포트>` 로 PID 확인
4. 호스트 포트를 다른 번호로 옮겨 격리

Postgres(5432), Redis(6379), MySQL(3306), Kafka(9092) — 표준 포트를 쓰는 모든 컨테이너 서비스에 동일 적용.

## 면접 답변용

> *"로컬에서 'auth failed' 보이면 자격증명부터 의심하는 게 본능인데, 사실 가장 많이 막히는 건 포트 충돌입니다. 한 머신에서 여러 프로젝트를 돌리면 같은 표준 포트를 쓰는 컨테이너가 흔하거든요. 디버깅 핵심은 *"호스트와 컨테이너 안에서 같은 명령으로 인증 결과가 다른지"* 확인하는 거고, 다르면 99% 다른 서비스에 닿고 있는 겁니다. 그래서 docker-compose 의 호스트 포트는 표준 포트 - 1 또는 +1 로 미리 분리하는 게 운영 안전 습관입니다."*
