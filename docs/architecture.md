# Backend Live Lab - Architecture Document

> 민야령의 백엔드 포트폴리오 프로젝트. 경력 9년 6개월의 EAI/MSA/백엔드 역량을
> 실행 가능한 데모와 운영 기록으로 설명하기 위해 직접 배포·개선하는 프로젝트.

---

## 1. 프로젝트 개요

| 항목 | 내용 |
|------|------|
| 목적 | 백엔드 엔지니어 포트폴리오 — 기술을 글이 아닌 실동작 시스템으로 증명 |
| 도메인 | https://minya.life |
| 소스 | https://github.com/minya8703/live-lab |
| 핵심 원칙 | Vanilla JS 프론트(의도적) · 백엔드 스포트라이트 · AWS 최소비용 |

### 기술 스택 요약

```
Backend   : Java 21, Spring Boot 3.4, Spring Data JPA, Spring Cache, Spring AI
Database  : PostgreSQL 16, Redis 7
Messaging : Apache Kafka 3.7 (KRaft, Zookeeper-free)
Storage   : AWS S3 (블로그 이미지)
AI        : Google Gemini 2.5 Flash (Spring AI, OpenAI 호환 엔드포인트)
Auth      : Google OAuth 2.0 + JWT (HMAC 서명)
Monitoring: Spring Actuator + Micrometer (Prometheus/Grafana는 로컬 선택 프로필, AWS 운영 제외)
Infra     : Docker Compose, GitHub Actions CI/CD, AWS EC2 (t4g.small)
CDN/DNS   : Cloudflare (Full strict 전환 목표, 실제 적용 검증 전 보안 부채로 관리)
Frontend  : Vanilla JavaScript (React/Vue 의도적 미사용)
```

---

## 2. 아키텍처

### 2.1 레이어드 아키텍처

```
com.minyaryung.livelab
├── presentation/    ← REST Controller (HTTP 요청/응답)
├── application/     ← Service (비즈니스 로직)
├── domain/          ← Entity, DTO, Repository 인터페이스
└── infra/           ← 외부 시스템 연동, 설정, 보안
```

**설계 원칙과 현재 범위:**
- HTTP 경계, 유스케이스, 데이터·외부 연동 책임을 패키지로 구분
- OAuthVerifier, TokenProvider, FileStorage처럼 외부 공급자 경계에는 포트 인터페이스 적용
- JPA entity annotation과 Spring Data repository가 domain 패키지에 남아 있어 strict clean architecture나 순수 domain으로 설명하지 않음

### 2.2 시스템 구성도

```
                    ┌─────────────┐
                    │  Cloudflare │ ← SSL, CDN, DNS
                    │  (minya.life)│
                    └──────┬──────┘
                           │ HTTPS
                    ┌──────▼──────┐
                    │   EC2       │ t4g.small (ARM)
                    │  (Docker)   │
                    └──────┬──────┘
                           │
          ┌────────────────┼────────────────┐
          │                │                │
   ┌──────▼──────┐ ┌──────▼──────┐ ┌───────▼──────┐
   │ Spring Boot │ │  PostgreSQL │ │    Redis     │
   │   :8080     │ │    :5432    │ │    :6379     │
   └──────┬──────┘ └─────────────┘ └──────────────┘
          │
   ┌──────▼──────┐
   │    Kafka    │
   │    :9092    │
   └─────────────┘
          │
   ┌──────▼──────┐
   │  AWS S3     │ ← 블로그 이미지 저장
   └─────────────┘
   ┌──────▼──────┐
   │ Google API  │ ← Gemini AI + OAuth
   └─────────────┘
```

---

## 3. 핵심 기능 상세

### 3.1 기술 블로그 플랫폼

| 구성요소 | 기술 | 설명 |
|---------|------|------|
| 글 CRUD | Spring Data JPA + PostgreSQL | slug 기반 URL, 마크다운 작성, 서버 사이드 HTML 렌더링 |
| 이미지 업로드 | AWS S3 | 다중 업로드, 자동 썸네일, 드래그앤드롭/Ctrl+V/버튼 |
| 인증 | Google OAuth 2.0 + JWT | 블로그 주인만 작성/수정/삭제 가능 |
| 마크다운 | CommonMark 0.22 + GFM Tables | 서버 사이드 렌더링, OG 메타태그 SSR |
| 검색 | 클라이언트 사이드 필터링 | 제목/내용/태그 통합 검색, 태그 필터 |

**API 엔드포인트:**

```
GET    /api/blog              → 발행된 글 목록 (페이지네이션)
GET    /api/blog/{slug}       → 글 상세
POST   /api/blog              → 글 작성 (JWT 필요)
PUT    /api/blog/{slug}       → 글 수정 (JWT 필요)
DELETE /api/blog/{slug}       → 글 삭제 (JWT 필요)
POST   /api/blog/upload       → 이미지 업로드 (JWT 필요)
```

### 3.2 Redis 캐시 라이브 데모

실제 10만 건 상품 데이터로 캐시 적용 전/후 응답 시간을 비교하는 벤치마크.

```
GET  /api/redis-demo/categories  → 카테고리 목록
GET  /api/redis-demo/run         → 벤치마크 실행 (cache=true/false)
POST /api/redis-demo/evict       → 캐시 초기화
```

**구현 상세:**
- DataSeeder: 기동 시 10만 건 배치 인서트 (1,000건/배치)
- ProductService: `@Cacheable` 기반 읽기 전용 Cache-Aside, 비교 실험용 수동 `@CacheEvict(allEntries=true)`
- RedisCacheConfig: TTL 60초, JSON 직렬화, 장애 시 DB 폴백
- cache get·put은 fail-open, evict·clear 실패는 stale 은폐를 막기 위해 호출자에게 전파
- 공개 데모 보호: 연결 peer 기준 벤치마크 분당 400회, 캐시 전체 초기화 분당 10회 제한
- 프론트: 반복 횟수별 평균 응답 시간 시각화

### 3.3 Kafka 처리량 라이브 데모

주문 이벤트 발행 → 소비 → DLT 처리까지의 전체 흐름을 실시간으로 보여주는 데모.

```
POST /api/kafka-demo/publish?count=1000  → 이벤트 발행
GET  /api/kafka-demo/status?runId=...     → 해당 실행 처리 현황
POST /api/kafka-demo/reset?runId=...      → 완료된 해당 실행 메트릭 초기화
```

**구현 상세:**
- KRaft 모드 (Zookeeper 없음), 3파티션
- OrderConsumer: 1/17 확률로 의도적 실패 → 재시도 3회 → DLT
- KafkaMetricsService: UUID runId 격리, 단일 active 실행, 늦은 callback 제외, 초당 처리량 계산
- DefaultErrorHandler + DeadLetterPublishingRecoverer
- 공개 데모 보호: 요청당 최대 2,000건, 연결 peer 기준 10분간 누적 5,000건 제한

### 3.4 AI 챗봇 (경력 Q&A)

경력 데이터 기반 질의응답. 모델 응답을 구조화하고 서버가 source ID를 검증해 자유 형식의 근거 없는 답변을 그대로 노출하지 않는다.

```
POST /api/chat  → 질문 (500자 제한, IP당 20회/시간)
```

**구현 상세:**
- Google Gemini 2.5 Flash (Spring AI, OpenAI 호환 엔드포인트)
- SystemPromptBuilder: 경력 마크다운 전체를 시스템 프롬프트로 주입
- 응답 계약: `{answer, sources, grounded}` JSON
- CareerDataLoader: 공개 Markdown의 상대 경로를 제공하고, ChatService는 시작 시 source ID snapshot을 고정해 시스템 프롬프트와 같은 재시작 주기로 검증
- ChatService: `grounded=true`의 source 존재를 검증하고 형식 오류·빈 출처·허위 파일명은 고정 보류 응답으로 전환
- 화면: 서버에서 검증한 source ID만 답변 아래 표시
- SimpleRateLimiter: peer 주소별 1시간 fixed window (20회), 비활성 bucket 정리, forwarded header 미신뢰
- 질문 원문과 provider 오류 body는 로그에 남기지 않고 길이·지연·상태·오류 타입만 기록
- 남은 한계: source 파일의 존재는 검증하지만 source 내용과 답변 주장 간 의미 일치는 아직 자동 검증하지 않음
- 재시도: 3회, 1s→2s→8s 백오프

### 3.5 경력 페이지

기존 사이트(minya8703.github.io)의 디자인을 live-lab 색감으로 변환하여 이식.

```
GET /api/career  → 구조화된 경력 데이터 (프로필, 기술스택, 대표 프로젝트 10건, 경력)
```

**구현 상세:**
- data/career/ 디렉토리의 마크다운 파일을 파싱
- YAML 프론트매터로 메타데이터 관리 (slug, title, date, tags, visible)
- 정적 HTML로 전환 (API 의존 제거) — 프로젝트 카드 토글, 이미지 모달, 타임라인
- fade-in 스크롤 애니메이션

### 3.6 DevLog & Ops

개발 과정의 의사결정 기록과 운영 인시던트 포스트모텀.

```
GET /api/devlog  → 개발 일지 목록
GET /api/ops     → 운영 기록 (incident / runbook)
```

- DevLog 10편: AI-DLC 방법론, 랜딩 디자인, 챗봇 정직 정책, LLM 마이그레이션, 인코딩 버그, Kafka 튜닝 등
- Ops 10편: SSH 키 이슈, Cloudflare SSL, 포트 충돌, Kafka advertised listener 트랩 등

---

## 4. 인증 & 보안

### 4.1 인증 흐름

```
1. 프론트 → GET /api/auth/client-id → Google OAuth Client ID
2. 프론트 → Google Sign-In 위젯 → Google credential JWT
3. 프론트 → POST /api/auth/google { credential } → 서버 검증
4. 서버  → GoogleOAuthVerifier: Google 토큰 검증
5. 서버  → JwtTokenProvider: 앱 JWT 발급 (email, name, picture)
6. 서버  → JWT는 HttpOnly·Secure·SameSite=Strict 쿠키, CSRF 값은 별도 Strict 쿠키로 전달
7. 프론트 → JWT를 읽지 않고 상태 변경 요청에 CSRF 쿠키 값을 `X-CSRF-Token` 헤더로 반영
8. 배포용 동기화 스크립트만 환경변수 Bearer 토큰을 사용하며 브라우저 쿠키 경로와 분리
```

### 4.2 보안 설계

| 항목 | 구현 |
|------|------|
| JWT 서명 | HMAC 서명, 24시간 만료, 운영 `JWT_SECRET` 필수·기본값 없음 |
| 마스터 체크 | JwtAuthInterceptor에서 이메일 일치 검증 |
| SQL Injection | JPA 파라미터화 쿼리 |
| XSS | Markdown raw HTML escape·위험 URL 제거, OG escape, LLM 출력 `textContent` |
| 세션 토큰 보호 | 브라우저 JWT는 HttpOnly·Secure·SameSite=Strict 쿠키에 저장해 JavaScript 접근 차단 |
| CSRF | 상태 변경 요청에서 Strict CSRF 쿠키와 `X-CSRF-Token` 헤더를 상수 시간 비교. Bearer 자동화 요청은 쿠키 인증과 분리 |
| 입력 검증 | 채팅 500자, Kafka count 1~2000, Redis iterations 1~200, 블로그 제목 300자·본문 100,000자·slug/URL 패턴 |
| 공개 데모 자원 제한 | 신뢰하지 않는 전달 헤더 대신 TCP peer 주소 사용. Kafka 5,000건/10분, Redis 실행 400회/분·전체 초기화 10회/분 |
| 브라우저 보안 헤더 | CSP로 script·frame 출처 제한, framing 차단, MIME sniffing 차단, Referrer/Permissions Policy 적용. Google OAuth와 Cloudflare Web Analytics는 정확한 origin만 허용하고 전체 `https:` script 허용은 금지. 프록시 TLS 종료를 고려해 HSTS 헤더를 항상 전달하며 브라우저는 HTTPS 응답에서만 수용 |
| 파일 검증 | 10MB 제한, PNG/JPEG/GIF/WebP signature 검사, 서버 결정 MIME·확장자, SVG/HTML 거부 |
| 비밀 관리 | .env 파일, 코드/git에 미포함 |
| Rate Limiting | TCP peer 주소 기반 fixed window (채팅 20회/시간, 비활성 bucket 정리, forwarded header 미신뢰) |
| 인증 시도 제한 | Google credential 검증 전에 TCP peer 기준 10분 10회로 제한하고 초과 요청은 429 반환 |
| 관리자 감사 로그 | 성공한 작성·수정·삭제·업로드의 작업 종류만 기록. 이메일·JWT·slug·본문·원본 파일명 제외 |
| 관리 API 자원 경계 | JSON 256KB·단일 문자열 120,000자·중첩 20단계, 페이지 0~1000·size 1~50, 이미지 업로드 peer당 20회/시간 |
| 블로그 정합성 | 애플리케이션 중복 확인 + DB unique constraint로 slug race 방어, 중복 409·없는 수정/삭제 404, 감사 로그는 성공 후 기록 |

HttpOnly 쿠키는 JavaScript를 통한 JWT 직접 탈취 가능성을 줄이지만 XSS 자체를 해결하지는 않는다. 따라서 CSP와 출력 인코딩을 함께 유지하며, 인증 범위가 확장되면 커스텀 interceptor에서 Spring Security의 표준 필터·CSRF 저장소로 전환한다.

---

## 5. 인프라 & 배포

### 5.1 Docker Compose 서비스

| 서비스 | 이미지 | 용도 |
|--------|--------|------|
| app | eclipse-temurin:21 (멀티스테이지) | Spring Boot 앱 |
| postgres | postgres:16-alpine | 블로그/상품 데이터 |
| redis | redis:7-alpine | 캐시 데모 |
| kafka | apache/kafka:3.7.0 | 메시지 스트리밍 데모 |
| prometheus | prom/prometheus:v2.51.0 | 로컬 선택 프로필(`monitoring`)의 메트릭 수집 |
| grafana | grafana/grafana:10.4.2 | 로컬 선택 프로필(`monitoring`)의 대시보드 |

### 5.2 Dockerfile (멀티스테이지)

```dockerfile
# Stage 1: Build — JDK 21, Gradle 빌드, 캐시 버스팅
# Stage 2: Runtime — JRE 21, JAR + data/ 복사
# JVM: -XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8
```

**캐시 버스팅:** 빌드 시 `sed`로 HTML 내 CSS/JS 참조에 `?v=<git-hash>` 주입

### 5.3 CI/CD (GitHub Actions)

```
Push to main
    │
    ▼
[CI] ./gradlew check (현재 단위 테스트, Testcontainers 통합 테스트는 architecture-improvement-roadmap.md의 K-04/R-02 예정)
    │ success
    ▼
[Deploy] SSH → EC2 → git pull → docker build --no-cache → up -d
    │
    ▼
[Health Check] curl /actuator/health (120초 타임아웃, 5초 간격)
```

### 5.4 모니터링

- **AWS 운영:** t4g.small 2GiB에서 핵심 4개 컨테이너(Spring Boot·Postgres·Redis·Kafka)의 자원을 우선하기 위해 Prometheus/Grafana 컨테이너 제외
- **운영 기본 노출:** Spring Actuator `health`, `info`만 공개. `metrics`, `prometheus`는 기본 프로필에서 제외
- **로컬 선택 구성:** 앱을 Spring `monitoring` 프로필로 실행하고 `docker compose --profile monitoring up -d`를 사용하면 Prometheus의 7일 보관과 Grafana 대시보드 검증 가능
- **감수한 trade-off:** 운영 시계열·Consumer Lag·캐시 히트율 대시보드를 상시 보지 못함. 인스턴스 상향 또는 외부 관측 환경 분리 시 재도입

### 5.5 네트워크 노출 경계

- 외부 공개가 필요한 애플리케이션 포트만 호스트 인터페이스에 publish한다.
- PostgreSQL·Redis·Kafka 호스트 개발 포트는 `127.0.0.1`에만 bind하고, 컨테이너 앱은 Docker network의 서비스 이름으로 통신한다.
- Kafka는 컨테이너용 `kafka:9092`와 호스트용 `localhost:${KAFKA_HOST_PORT}` listener를 분리해 client가 실제 도달 가능한 주소를 각각 광고한다.
- Prometheus·Grafana도 로컬 `monitoring` 프로필에서 loopback으로만 접근한다. Grafana 익명 Viewer 설정이 외부 공개로 이어지지 않게 한다.
- AWS Security Group과 loopback bind를 함께 적용해 네트워크 경계를 이중화한다.

---

## 6. 데이터 구조

### 6.1 PostgreSQL 테이블

**blog_posts**
```sql
id             SERIAL PRIMARY KEY
slug           VARCHAR(200) UNIQUE
title          VARCHAR
summary        VARCHAR
content        TEXT          -- 마크다운 원본
html_content   TEXT          -- 렌더링된 HTML
thumbnail_url  VARCHAR
tags           VARCHAR
published      BOOLEAN
created_at     TIMESTAMP
updated_at     TIMESTAMP
```

**product** (Redis 데모용, 10만 건)
```sql
id             SERIAL PRIMARY KEY
name           VARCHAR(80)
category       VARCHAR(30)
sub_category   VARCHAR(30)
price          DECIMAL(12,2)
stock          INT
```

### 6.2 마크다운 데이터 (data/)

```
data/
├── career/           ← Markdown 기반 경력 원문
│   ├── profile.md
│   ├── tech-stack.md
│   ├── summary.md
│   ├── projects/     ← 대표 프로젝트 10건
│   └── experience/   ← 경력 4사
├── devlog/           ← 개발 일지 10편
├── ops/              ← 운영 기록
│   ├── incident/     ← 인시던트 9건
│   └── runbook/      ← 운영 매뉴얼 1건
└── notes/            ← 면접 준비
```

YAML 프론트매터 예시:
```yaml
---
slug: hanssem-eai
title: 한샘 webMethods EAI SAP 연동
date: 2026-03-01
tags: [EAI, SAP, webMethods]
visible: true
---
```

---

## 7. 프론트엔드

### 의도적으로 Vanilla JavaScript

React/Vue/Angular를 사용하지 않는 것은 의도적 설계 판단:
- 백엔드 엔지니어 포트폴리오이므로 프론트엔드 프레임워크 의존 최소화
- 번들러/트랜스파일러 없이 브라우저에서 직접 실행
- 백엔드 아키텍처와 기술 선택에 스포트라이트

### 페이지 구성

| 페이지 | 경로 | 용도 |
|--------|------|------|
| 랜딩 | `/` | 히어로, Live Lab 카드, Proof 섹션 |
| 경력 | `/career.html` | 전체 경력 (대표 프로젝트 10건, 타임라인) |
| 블로그 목록 | `/blog.html` | velog 스타일 카드, 검색, 태그 필터 |
| 블로그 상세 | `/blog/post.html?slug=xxx` | 글 본문, OG 메타태그 SSR |
| 글쓰기 | `/blog/write.html` | 마크다운 에디터, 이미지 업로드 |
| 챗봇 | `/lab/chat.html` | 경력 Q&A AI 챗봇 |
| Redis 데모 | `/lab/redis.html` | 캐시 벤치마크 |
| Kafka 데모 | `/lab/kafka.html` | 처리량 데모 |
| DevLog | `/lab/devlog.html` | 개발 일지 |
| Ops | `/lab/ops.html` | 운영 기록 |

---

## 8. 설계 패턴

| 패턴 | 적용 위치 |
|------|----------|
| Layered Architecture | 전체 패키지 구조 |
| Strategy | OAuthVerifier, TokenProvider, FileStorage 인터페이스 |
| Repository | JPA Repository 추상화 |
| Interceptor | JwtAuthInterceptor (인증 체크) |
| Dead Letter | Kafka DLT (실패 메시지 격리) |
| Rate Limiter | SimpleRateLimiter (인메모리 fixed window) |
| Cache-Aside | ProductService (@Cacheable) |
| Graceful Degradation | S3 미설정 시 null bean, Redis 장애 시 DB 폴백 |
| 제한된 Retry | Spring AI 재시도 (최대 3회, 지수 백오프). Circuit Breaker는 미구현 |

---

## 9. API 엔드포인트 전체 목록

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| GET | `/api/auth/client-id` | - | Google OAuth Client ID |
| POST | `/api/auth/google` | - | OAuth 로그인 |
| GET | `/api/auth/me` | HttpOnly JWT 쿠키 | 현재 사용자 정보 |
| POST | `/api/auth/logout` | JWT 쿠키 + CSRF | 인증 쿠키 만료 |
| GET | `/api/blog` | - | 발행된 글 목록 |
| GET | `/api/blog/{slug}` | - | 글 상세 |
| POST | `/api/blog` | JWT 쿠키+CSRF 또는 자동화 Bearer | 글 작성 |
| PUT | `/api/blog/{slug}` | JWT 쿠키+CSRF 또는 자동화 Bearer | 글 수정 |
| DELETE | `/api/blog/{slug}` | JWT 쿠키+CSRF 또는 자동화 Bearer | 글 삭제 |
| POST | `/api/blog/upload` | JWT 쿠키+CSRF 또는 자동화 Bearer | 이미지 업로드 |
| GET | `/api/career` | - | 경력 페이지 데이터 |
| GET | `/api/devlog` | - | 개발 일지 |
| GET | `/api/ops` | - | 운영 기록 |
| POST | `/api/chat` | - | AI 챗봇 질문 → `answer/sources/grounded` (Rate Limited) |
| POST | `/api/kafka-demo/publish` | - | Kafka 이벤트 발행 |
| GET | `/api/kafka-demo/status?runId=...` | - | 해당 Kafka 실행 처리 현황 |
| POST | `/api/kafka-demo/reset?runId=...` | - | 완료된 Kafka 실행 초기화 |
| GET | `/api/redis-demo/categories` | - | 상품 카테고리 |
| GET | `/api/redis-demo/run` | - | 캐시 벤치마크 |
| POST | `/api/redis-demo/evict` | - | 캐시 초기화 |
| GET | `/api/status` | - | 빌드 진행 상태 |

---

## 10. 환경 변수

```properties
# Database
POSTGRES_DB=livelab
POSTGRES_USER=livelab
POSTGRES_PASSWORD=<password>
POSTGRES_HOST_PORT=5433

# Redis / Kafka (호스트 개발용 loopback 포트)
REDIS_HOST_PORT=6379
KAFKA_HOST_PORT=9092

# Monitoring (로컬 선택 프로필)
PROMETHEUS_HOST_PORT=9090
GRAFANA_HOST_PORT=3001
APP_HOST_PORT=80

# Auth
GOOGLE_CLIENT_ID=<google-oauth-client-id>
BLOG_MASTER_EMAIL=minya8703@gmail.com
JWT_SECRET=<random-32-char-string>

# AI
GOOGLE_API_KEY=<gemini-api-key>

# Storage (S3)
STORAGE_ENDPOINT=https://s3.ap-northeast-2.amazonaws.com
STORAGE_REGION=ap-northeast-2
STORAGE_ACCESS_KEY=<aws-access-key>
STORAGE_SECRET_KEY=<aws-secret-key>
STORAGE_BUCKET=livelab-blog-minya
STORAGE_PUBLIC_URL=https://livelab-blog-minya.s3.ap-northeast-2.amazonaws.com
```

---

## 11. 로컬 개발 환경

```bash
# 1. 기본 의존 서비스 기동 (Postgres, Redis, Kafka)
docker compose up -d

# 선택: Prometheus, Grafana까지 로컬에서 기동
docker compose --profile monitoring up -d

# 다른 터미널에서 metrics/prometheus endpoint를 여는 로컬 전용 프로필로 앱 실행
./gradlew bootRun --args='--spring.profiles.active=monitoring'

# 2. .env 파일 준비
cp .env.example .env
# → 실제 값 입력

# 3. 앱 실행
./gradlew bootRun

# 4. 접속
open http://localhost:8080
```

**운영 배포:**
```bash
# EC2에서
docker compose --profile prod up -d
```

---

*이 문서는 프로젝트 구조와 기술 선택을 설명하기 위해 작성되었습니다.*
*최종 갱신: 2026-07-11*
