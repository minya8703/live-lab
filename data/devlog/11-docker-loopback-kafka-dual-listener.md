---
slug: docker-loopback-kafka-dual-listener
unit: 8
title: 데이터 포트를 loopback으로 닫고 Kafka listener를 둘로 나눈 이유
date: 2026-08-09
tags: [docker, kafka, security, architecture]
---

## 맥락

로컬 개발에서는 Spring Boot를 호스트에서 실행하고 PostgreSQL·Redis·Kafka는 Docker Compose로 실행한다.
운영에서는 Spring Boot까지 컨테이너로 실행하므로 같은 Kafka broker에 접근하는 클라이언트의 네트워크 관점이 다르다.

- 호스트 앱이 도달할 수 있는 주소: `localhost:9092`
- 컨테이너 앱이 도달할 수 있는 주소: `kafka:9092`

Kafka는 bootstrap 연결 후 broker가 돌려준 metadata의 주소로 다시 연결한다. 따라서 최초 접속 주소뿐 아니라
`advertised.listeners`가 각 클라이언트에서 실제로 도달 가능한 주소인지가 중요하다.

## 발견한 문제

기존 구성은 컨테이너 앱을 위해 `kafka:9092`만 광고했다. 운영 컨테이너에서는 맞지만 호스트에서 실행한
Spring Boot는 Docker 내부 DNS 이름 `kafka`를 해석할 수 없다. 반대로 `localhost:9092`만 광고하면
컨테이너 앱이 Kafka 컨테이너가 아니라 자기 컨테이너를 찾는다.

또한 다음과 같은 짧은 포트 매핑은 기본적으로 호스트의 모든 네트워크 인터페이스에 포트를 연다.

```yaml
ports:
  - "6379:6379"
```

EC2 Security Group이 막고 있더라도 Redis·Kafka처럼 별도 인증을 구성하지 않은 내부 서비스가 호스트 외부
인터페이스에서 listen하는 상태는 불필요한 위험이다. 보안 그룹 오설정 하나가 곧바로 데이터 서비스 노출로
이어지지 않도록 호스트 수준의 두 번째 경계가 필요했다.

## 결정

Kafka listener를 클라이언트 네트워크별로 분리했다.

```yaml
KAFKA_LISTENERS: >-
  PLAINTEXT://0.0.0.0:9092,
  PLAINTEXT_HOST://0.0.0.0:9094,
  CONTROLLER://0.0.0.0:9093
KAFKA_ADVERTISED_LISTENERS: >-
  PLAINTEXT://kafka:9092,
  PLAINTEXT_HOST://localhost:9092
```

호스트의 `127.0.0.1:9092`는 컨테이너의 HOST listener 9094로 연결하고, 컨테이너 앱은 publish 포트를
거치지 않고 Docker network의 `kafka:9092`로 직접 연결한다.

PostgreSQL·Redis·Kafka·Prometheus·Grafana의 호스트 포트도 모두 loopback으로 제한했다.

```yaml
postgres:
  ports:
    - "127.0.0.1:${POSTGRES_HOST_PORT:-5433}:5432"

redis:
  ports:
    - "127.0.0.1:${REDIS_HOST_PORT:-6379}:6379"

kafka:
  ports:
    - "127.0.0.1:${KAFKA_HOST_PORT:-9092}:9094"
```

## 아키텍처 관점

`LISTENERS`는 broker가 실제로 수신하는 주소이고, `ADVERTISED_LISTENERS`는 클라이언트에게 다음 연결
주소로 알려주는 값이다. 이 둘을 단순히 같은 문자열로 맞추는 것이 아니라 클라이언트가 속한 네트워크에서
도달 가능한 주소를 광고해야 한다.

보안 경계도 한 계층에만 의존하지 않는다.

1. AWS Security Group에서 외부 인바운드를 제한한다.
2. Docker publish 주소를 `127.0.0.1`로 제한한다.
3. 컨테이너 간 통신은 외부 포트가 아닌 격리된 Docker network를 사용한다.

## 검증

`docker compose --profile monitoring config`의 최종 렌더링 결과로 다음을 확인했다.

- PostgreSQL `127.0.0.1:5433 → 5432`
- Redis `127.0.0.1:6379 → 6379`
- Kafka `127.0.0.1:9092 → 9094`
- Prometheus `127.0.0.1:9090 → 9090`
- Grafana `127.0.0.1:3001 → 3000`
- Kafka 내부 광고 주소 `kafka:9092`
- Kafka 호스트 광고 주소 `localhost:9092`

전체 JUnit 테스트도 통과해 Spring 설정과 기존 기능에 회귀가 없음을 확인했다.

## 감수한 trade-off와 남은 한계

- 다른 PC에서 DB·Kafka에 직접 접속할 수 없으므로 필요한 경우 SSH tunnel을 명시적으로 사용해야 한다.
- Kafka 통신은 격리된 단일 호스트 Docker network 안에서 PLAINTEXT다. 다중 호스트나 신뢰 경계 밖으로
  확장한다면 SASL/TLS와 ACL을 추가해야 한다.
- loopback bind는 Security Group을 대체하지 않는다. 두 경계를 함께 유지해야 한다.

작은 단일 서버에서도 포트를 전부 여는 것보다 필요한 통신 경로만 남기는 편이 운영 실수를 견디는 구조다.
