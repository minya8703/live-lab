# 민야령 · Backend Live Lab

[![CI](https://github.com/minya8703/live-lab/actions/workflows/ci.yml/badge.svg)](https://github.com/minya8703/live-lab/actions/workflows/ci.yml)

> "엔터프라이즈 시스템을 연결하고 안정화하는 백엔드 엔지니어"의 라이브 작업실.
> Kafka · Redis · MSA를 라이브로 증명하고, AI-DLC 사이클로 만들어가는 과정을 그대로 노출합니다.

— [기존 정적 포트폴리오 (이력 상세)](https://minya8703.github.io/) · 메일: minya8703@gmail.com

## 무엇을 보여주나

| Unit | 라이브 | 어필 포인트 |
|---|---|---|
| U3 | `/lab/chat` | AI 경력 Q&A — Spring AI + Gemini 2.5 Flash, 없는 경험은 솔직히 "없다"고 답함 |
| U4 | `/lab/redis` | Redis 캐시 라이브 비교 — Postgres 100k건 + CacheErrorHandler fallback |
| U5 | `/lab/kafka` | Kafka throughput + DLT 라이브 — FixedBackOff 재시도 → DLT 라우팅 |
| U6 | `/lab/grafana` | Prometheus + Grafana로 JVM·HTTP·Cache·Kafka 메트릭 임베드 |
| U7 | `/lab/devlog` | AI-DLC 개발 일지 — 실제 디버깅·결정의 마크다운 누적 |

## 기술 스택

- **Backend**: Java 21, Spring Boot 3.4, Spring AI, Spring Kafka, Spring Cache
- **Data**: Postgres 16, Redis 7, Apache Kafka 3.7 (KRaft)
- **Observability**: Spring Boot Actuator, Micrometer, Prometheus, Grafana
- **Frontend**: Vanilla JS + HTML/CSS (의도적으로 프레임워크 미도입 — 백엔드 스포트라이트)
- **Test**: JUnit 5, Testcontainers (실 인프라 통합 테스트)
- **CI**: GitHub Actions

## 로컬 실행

전제: Docker Desktop · Java 21 · Gradle (또는 Gradle Wrapper)

```bash
# 1. .env 생성 (애플리케이션 + docker-compose 가 같이 읽음)
cp .env.example .env
# .env 의 GOOGLE_API_KEY 에 https://aistudio.google.com/app/apikey 에서 발급한 키 입력

# 2. 외부 의존성 기동
docker compose up -d
docker compose ps   # postgres / redis / kafka 모두 healthy 확인

# 3. 애플리케이션 실행
./gradlew bootRun

# → http://localhost:8089
```

## 테스트

```bash
# 단위 + 통합 테스트 (Testcontainers 가 진짜 Postgres/Redis/Kafka 컨테이너 spin-up)
./gradlew check
```

CI 환경(GitHub Actions Ubuntu)에서 push 마다 자동 실행됩니다. 상세 결과는 위 CI 배지를 클릭.

## 디자인 결정

각 Unit의 *왜* 와 디버깅 회고는 [docs/inception.md](docs/inception.md)와 [data/devlog/](data/devlog/)에 누적합니다. 사이트의 `/lab/devlog`에서도 동일 내용을 마크다운 → HTML 렌더로 노출합니다.

특히 가치 있는 회고:
- [Postgres 포트 충돌 1시간 디버깅](data/devlog/04-redis-port-conflict.md) — "auth failed"가 인증 문제가 아닐 때
- [Bitnami 이미지 사라짐](data/devlog/06-kafka-bitnami-gone.md) — 벤더 락인은 의외의 곳에서
- [Grafana "No data" 캐시 lazy binding](data/devlog/08-grafana-cache-binding.md)

## 라이센스 / 컨택트

이 저장소는 포트폴리오 목적의 공개 소스입니다. 채용·협업 관련 문의는 minya8703@gmail.com 으로 부탁드립니다.
