# Backend Live Lab - Architecture Document

> 민야령의 백엔드 포트폴리오 프로젝트. 경력 9년 6개월의 EAI/MSA/백엔드 역량을
> "라이브로 증명"하기 위해, 실제 운영 가능한 수준의 시스템을 설계·구현한 프로젝트.

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
Auth      : Google OAuth 2.0 + JWT (HMAC-SHA256)
Monitoring: Spring Actuator + Micrometer (Prometheus/Grafana는 로컬 선택 프로필, AWS 운영 제외)
Infra     : Docker Compose, GitHub Actions CI/CD, AWS EC2 (t4g.small)
CDN/DNS   : Cloudflare (SSL Full Strict)
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

**설계 원칙:**
- Presentation → Application → Domain → Infra 단방향 의존
- Domain 계층은 외부 의존 없음 (순수 Java)
- Strategy 패턴으로 외부 시스템 교체 용이 (OAuthVerifier, TokenProvider, FileStorage)

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
- ProductService: `@Cacheable` / `@CacheEvict` 기반
- RedisCacheConfig: TTL 60초, JSON 직렬화, 장애 시 DB 폴백
- 프론트: 반복 횟수별 평균 응답 시간 시각화

### 3.3 Kafka 처리량 라이브 데모

주문 이벤트 발행 → 소비 → DLT 처리까지의 전체 흐름을 실시간으로 보여주는 데모.

```
POST /api/kafka-demo/publish?count=1000  → 이벤트 발행
GET  /api/kafka-demo/status              → 처리 현황
POST /api/kafka-demo/reset               → 메트릭 초기화
```

**구현 상세:**
- KRaft 모드 (Zookeeper 없음), 3파티션
- OrderConsumer: 1/17 확률로 의도적 실패 → 재시도 3회 → DLT
- KafkaMetricsService: 발행/성공/DLT 카운트, 초당 처리량 계산
- DefaultErrorHandler + DeadLetterPublishingRecoverer

### 3.4 AI 챗봇 (경력 Q&A)

경력 데이터 기반 질의응답. 환각(hallucination) 방지를 핵심 설계 원칙으로.

```
POST /api/chat  → 질문 (500자 제한, IP당 20회/시간)
```

**구현 상세:**
- Google Gemini 2.5 Flash (Spring AI, OpenAI 호환 엔드포인트)
- SystemPromptBuilder: 경력 마크다운 전체를 시스템 프롬프트로 주입
- 규칙: 데이터에 없는 내용은 "해당 정보가 없습니다"로 답변
- SimpleRateLimiter: IP별 토큰 버킷 (20회/시간)
- 재시도: 3회, 1s→2s→8s 백오프

### 3.5 경력 페이지

기존 사이트(minya8703.github.io)의 디자인을 live-lab 색감으로 변환하여 이식.

```
GET /api/career  → 구조화된 경력 데이터 (프로필, 기술스택, 프로젝트 8건, 경력 4사)
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
6. 프론트 → localStorage에 JWT 저장
7. API 호출 시 Authorization: Bearer <JWT> 헤더 포함
```

### 4.2 보안 설계

| 항목 | 구현 |
|------|------|
| JWT 서명 | HMAC-SHA256, 설정 가능한 시크릿 |
| 마스터 체크 | JwtAuthInterceptor에서 이메일 일치 검증 |
| SQL Injection | JPA 파라미터화 쿼리 |
| XSS | 서버 사이드 HTML 이스케이프 (OG 태그) |
| CSRF | 무상태 JWT (세션 쿠키 없음) |
| 입력 검증 | 채팅 500자, Kafka count 1~10000 |
| 파일 검증 | S3FileStorage에서 파일명 새니타이징 |
| 비밀 관리 | .env 파일, 코드/git에 미포함 |
| Rate Limiting | IP 기반 토큰 버킷 (채팅 20회/시간) |

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
- **유지한 계측:** Spring Actuator의 health/info/prometheus/metrics 엔드포인트와 Micrometer 커스텀 메트릭
- **로컬 선택 구성:** `docker compose --profile monitoring up -d`로 Prometheus의 7일 보관과 Grafana 대시보드 검증 가능
- **감수한 trade-off:** 운영 시계열·Consumer Lag·캐시 히트율 대시보드를 상시 보지 못함. 인스턴스 상향 또는 외부 관측 환경 분리 시 재도입

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
├── career/           ← 경력 데이터 (20개 파일)
│   ├── profile.md
│   ├── tech-stack.md
│   ├── summary.md
│   ├── projects/     ← 프로젝트 8건
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
| 경력 | `/career.html` | 전체 경력 (프로젝트 8건, 타임라인) |
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
| Rate Limiter | SimpleRateLimiter (토큰 버킷) |
| Cache-Aside | ProductService (@Cacheable) |
| Graceful Degradation | S3 미설정 시 null bean, Redis 장애 시 DB 폴백 |
| Circuit Breaker | Spring AI 재시도 (3회, 지수 백오프) |

---

## 9. API 엔드포인트 전체 목록

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| GET | `/api/auth/client-id` | - | Google OAuth Client ID |
| POST | `/api/auth/google` | - | OAuth 로그인 |
| GET | `/api/auth/me` | JWT | 현재 사용자 정보 |
| GET | `/api/blog` | - | 발행된 글 목록 |
| GET | `/api/blog/{slug}` | - | 글 상세 |
| POST | `/api/blog` | JWT | 글 작성 |
| PUT | `/api/blog/{slug}` | JWT | 글 수정 |
| DELETE | `/api/blog/{slug}` | JWT | 글 삭제 |
| POST | `/api/blog/upload` | JWT | 이미지 업로드 |
| GET | `/api/career` | - | 경력 페이지 데이터 |
| GET | `/api/devlog` | - | 개발 일지 |
| GET | `/api/ops` | - | 운영 기록 |
| POST | `/api/chat` | - | AI 챗봇 질문 (Rate Limited) |
| POST | `/api/kafka-demo/publish` | - | Kafka 이벤트 발행 |
| GET | `/api/kafka-demo/status` | - | Kafka 처리 현황 |
| POST | `/api/kafka-demo/reset` | - | Kafka 메트릭 초기화 |
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

# Kafka
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
