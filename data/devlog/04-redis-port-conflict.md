---
slug: postgres-port-conflict
unit: 4
title: "auth failed"의 원인이 인증 정보가 아니었던 경우 — 포트 충돌 진단
date: 2026-05-22
tags: [debugging, docker, postgres]
---

## 증상
Spring Boot 기동 시 인증 실패:

```
PSQLException: 치명적오류: 사용자 "livelab"의 password 인증을 실패했습니다
```

`.env`의 자격 증명과 컨테이너 초기화를 반복했지만 증상은 같았다.
반면 컨테이너 내부에서 `psql`로 같은 계정에 접속하면 성공했다.

## 검증한 가설
1. `.env` 인코딩 (UTF-16 BOM?) — 확인했더니 UTF-8.
2. `spring-dotenv` 로딩 실패 — `GOOGLE_API_KEY` 는 잘 읽고 있었다.
3. `${POSTGRES_PASSWORD:livelab}` placeholder 우선순위 — placeholder 빼고 직박해도 같은 에러.
4. PowerShell 환경 변수 우선순위 — `Remove-Item env:POSTGRES_PASSWORD` 해도 같은 에러.

## 결정적 단서
컨테이너 안에서는 인증 성공, 호스트에서는 실패. **같은 비번인데**.

이 차이로부터 호스트와 컨테이너가 서로 다른 Postgres에 연결될 가능성을 우선 확인했다.

## 실제 원인
사용자 PC에 **다른 프로젝트의 Postgres** 가 5432 포트를 이미 잡고 있었다
다른 프로젝트의 컨테이너와 포트 점유 상태를 확인해 이를 검증했다.
Spring 의 `localhost:5432` 가 우리 Docker 컨테이너가 아니라 그 다른 Postgres 로 가고 있었고,
거기엔 `livelab` 사용자가 없으니 인증 실패.

## 해결
호스트 포트를 5433으로 옮겨 다른 Postgres와 격리:

```yaml
ports:
  - "127.0.0.1:${POSTGRES_HOST_PORT:-5433}:5432"
```

```properties
spring.datasource.url=jdbc:postgresql://localhost:${POSTGRES_HOST_PORT:5433}/livelab
```

## 재사용 가능한 진단 기준

로컬에서 `auth failed` 또는 `connection refused`가 발생하면 자격 증명과 함께 실제 연결 대상도 확인한다.

호스트와 컨테이너에서 동일한 클라이언트·계정으로 접속한 결과가 다르면 DNS, 포트 매핑과 실제 리스닝 프로세스를 비교한다.

이 진단 순서는 Redis, MySQL, Kafka처럼 표준 포트가 다른 로컬 프로젝트와 충돌할 수 있는 서비스에도 적용할 수 있다.
