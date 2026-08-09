---
slug: kafka-advertised-listener-trap
title: Kafka ADVERTISED_LISTENERS — 첫 연결은 성공하고 그 다음부터 무한 retry 의 함정
date: 2026-06-10
tags: [kafka, docker, networking, debugging]
---

## 증상

EC2 재배포 후 https://minya.life/lab/kafka.html 의 발행 버튼이 **전혀 반응 없음**. 컨테이너는 다 `Up` 상태. 메모리도 여유 있음 (available 648 MB). 그런데 Spring Boot 의 Kafka 클라이언트가 무한히 WARN:

```
WARN  org.apache.kafka.clients.NetworkClient :
  [Consumer clientId=consumer-livelab-orders-consumer-1, groupId=livelab-orders-consumer]
  Connection to node 1 (localhost/127.0.0.1:9092) could not be established.
  Node may not be available.
```

초당 3~4 번씩 같은 WARN 만 반복.

## 의심한 가설 (시간순)

1. **Kafka 컨테이너 죽음** — `docker compose ps` 에 `Up` 표시 + 로그에 `Kafka Server started` 도 있음 → 제외
2. **메모리 부족 / OOM** — `free -h` 의 available 648 MB → 제외
3. **`SPRING_KAFKA_BOOTSTRAP_SERVERS` 환경변수 누락** — `docker exec livelab-app env | grep KAFKA` 로 `kafka:9092` 정상 주입 확인 → 제외
4. **네트워크 단절** — 같은 docker compose network 안 → 제외

남은 의문: *Spring 이 `kafka:9092` 로 설정됐는데 왜 WARN 메시지의 주소는 `localhost/127.0.0.1:9092` 인가?*

## 결정적 단서

**Spring 설정 (`kafka:9092`) ≠ WARN 메시지의 주소 (`localhost:9092`)** 비대칭.

→ Spring 이 첫 연결은 `kafka:9092` 로 성공했고, *그 다음* 어디선가 `localhost:9092` 로 retry 하고 있음. 그 주소를 *준 곳* 이 Kafka broker 자신 (metadata 응답) 밖에 없음.

`docker-compose.yml` 의 Kafka 환경 변수 확인:
```yaml
KAFKA_ADVERTISED_LISTENERS: "PLAINTEXT://localhost:9092"   # ❌
```

확정.

## 진짜 원인 — `KAFKA_ADVERTISED_LISTENERS` 의 역할

Kafka 의 두 listener 변수는 *완전히 다른 역할* 인데 이름이 비슷해서 헷갈리는 게 함정의 핵심.

| 변수 | 역할 |
|---|---|
| `KAFKA_LISTENERS` | broker 가 *실제로 bind* 하는 주소. 보통 `0.0.0.0:9092` (모든 인터페이스에서 listen) |
| **`KAFKA_ADVERTISED_LISTENERS`** | broker 가 **metadata 응답으로 client 에게 *알려주는*** 주소. *"다음부턴 이 주소로 와"* 의 안내 |

KRaft 모드 단일 노드 환경에서 일어나는 흐름:

```
1. Spring Boot: kafka:9092 로 첫 연결       → bootstrap 성공
2. Kafka broker (metadata 응답):
     "내 토픽 leader 는 node 1 이고,
      node 1 의 주소는 localhost:9092 야"   ← ADVERTISED_LISTENERS 값
3. Spring Boot: "OK, localhost:9092 로 갈게" → 자기 컨테이너 안 ::1 / 127.0.0.1 로 시도
4. localhost:9092 안 떠 있음 (Kafka 는 다른 컨테이너에)
5. retry → retry → retry → 무한 WARN
```

*"첫 연결은 성공하고 그 다음부터 안 됨"* 의 메시지가 결정적 단서였다.

## 해결

`docker-compose.yml`:

```yaml
# Before
KAFKA_ADVERTISED_LISTENERS: "PLAINTEXT://localhost:9092"

# After
KAFKA_ADVERTISED_LISTENERS: "PLAINTEXT://kafka:9092"
```

docker compose network 안에선 **서비스 이름이 hostname** 으로 자동 resolve 되므로 `kafka:9092` 가 정답. 재기동:

```bash
docker compose up -d --force-recreate kafka
docker compose restart app
```

수초 내 WARN 사라지고 `Discovered group coordinator kafka:9092` INFO 로 전환. 사이트의 발행 버튼이 즉시 살아남.

## 일반화한 룰

> **Kafka 의 ADVERTISED_LISTENERS 는 *client 가 도달 가능한 주소* 로 광고해야 한다 — broker 의 입장이 아니라 client 의 입장에서 본 주소.**

세 가지 단서 패턴:

1. **WARN 메시지의 주소가 설정한 bootstrap server 와 다르면** → ADVERTISED 의심. *"내가 설정 안 한 주소가 어디서 나오나?"* 의 답은 broker metadata 밖에 없음.
2. **첫 연결 성공 + 그 다음 retry 무한** → bootstrap 은 됐고 metadata 만 깨졌다는 신호.
3. **Docker network 안에선 서비스 이름이 hostname** — `localhost` 는 *자기 컨테이너* 라 쓰면 안 됨.

호스트 OS 와 컨테이너 안 양쪽에서 접속 필요한 경우 — 별도 listener 두 개:

```yaml
KAFKA_LISTENERS: "PLAINTEXT://0.0.0.0:9092,PLAINTEXT_HOST://0.0.0.0:9094,CONTROLLER://0.0.0.0:9093"
KAFKA_ADVERTISED_LISTENERS: "PLAINTEXT://kafka:9092,PLAINTEXT_HOST://localhost:9092"
KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: "CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT"
```

현재 이 사이트는 컨테이너 앱과 호스트에서 실행하는 로컬 Spring Boot를 모두 지원하므로 두 listener를 사용한다.
호스트의 `127.0.0.1:9092`를 컨테이너의 HOST listener `9094`로 publish해 외부 네트워크 노출 없이
기존 로컬 설정을 유지한다.
