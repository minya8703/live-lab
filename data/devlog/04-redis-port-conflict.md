---
slug: postgres-port-conflict
unit: 4
title: "auth failed" 가 인증 문제가 아닐 때 — 1시간을 잡아먹은 포트 충돌
date: 2026-05-22
tags: [debugging, docker, postgres]
---

## 증상
Spring Boot 기동 시 인증 실패:

```
PSQLException: 치명적오류: 사용자 "livelab"의 password 인증을 실패했습니다
```

`.env` 에 비번을 박았고, `docker compose down -v` 로 볼륨까지 지우고 다시 올렸다.
컨테이너 안에서 `psql` 로 같은 비번 접속은 **성공한다**.
그런데 Spring 만 실패. 30분 헤맸다.

## 의심한 가설 (모두 헛짚음)
1. `.env` 인코딩 (UTF-16 BOM?) — 확인했더니 UTF-8.
2. `spring-dotenv` 로딩 실패 — `GOOGLE_API_KEY` 는 잘 읽고 있었다.
3. `${POSTGRES_PASSWORD:livelab}` placeholder 우선순위 — placeholder 빼고 직박해도 같은 에러.
4. PowerShell 환경 변수 우선순위 — `Remove-Item env:POSTGRES_PASSWORD` 해도 같은 에러.

## 결정적 단서
컨테이너 안에서는 인증 성공, 호스트에서는 실패. **같은 비번인데**.

이 비대칭이 의미하는 단 하나의 시나리오는 —
*"호스트와 컨테이너가 서로 다른 Postgres 에 붙고 있다"*.

## 실제 원인
사용자 PC에 **다른 프로젝트의 Postgres** 가 5432 포트를 이미 잡고 있었다
(`docker volume ls` 에 `raonnanal-api_postgres_data` 가 보인 것이 결정적 힌트).
Spring 의 `localhost:5432` 가 우리 Docker 컨테이너가 아니라 그 다른 Postgres 로 가고 있었고,
거기엔 `livelab` 사용자가 없으니 인증 실패.

## 해결
호스트 포트를 5433으로 옮겨 다른 Postgres와 격리:

```yaml
ports:
  - "${POSTGRES_HOST_PORT:-5433}:5432"
```

```properties
spring.datasource.url=jdbc:postgresql://localhost:${POSTGRES_HOST_PORT:5433}/livelab
```

## 교훈 (메모리에 박았음)
**로컬에서 "auth failed" 나 "connection refused" 를 봤을 때
자격증명 디버깅 전에 포트 충돌부터 점검한다.**

호스트와 컨테이너에서 같은 클라이언트로 같은 인증을 시도해서 결과가 다르면
99% 다른 서비스에 연결 중이다.

Redis(6379), MySQL(3306), Kafka(9092) 모두 동일한 함정.
이 룰을 일찍 알았으면 30분을 아꼈다.
