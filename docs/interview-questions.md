# Live Lab 면접 예상 질문 & 답변 가이드

---

## 1. RAG 챗봇 구현

### Q1. RAG를 어떤 방식으로 구현했나요?

**A.** Static Context Injection 방식의 RAG입니다.
- `data/career/` 디렉토리에 마크다운 20개 파일(프로필, 프로젝트 8건, 경력 4건, 기술스택, FAQ 등)을 구조화해 두고
- 앱 기동 시 `CareerDataLoader`가 전체 파일을 읽어 `SystemPromptBuilder`에서 **규칙 9조 + 전체 경력 데이터**를 하나의 시스템 프롬프트(약 19,000자)로 결합합니다.
- 이후 사용자 질문마다 이 시스템 프롬프트와 함께 Google Gemini 2.5 Flash에 전달합니다.

**왜 벡터 DB가 아닌가?**
- 경력 데이터가 20개 파일(약 19KB)로 소규모 — 토큰 한도 내에서 전체 주입 가능
- 벡터 검색의 retrieval 오차(관련 없는 문서 반환)를 원천 차단
- 포트폴리오 특성상 데이터가 정적이고 변경 빈도가 낮아 기동 시 1회 로딩으로 충분

### Q2. 할루시네이션(환각) 방지는 어떻게 했나요?

**A.** 세 겹의 장치를 사용합니다.

1. **시스템 프롬프트 규칙 #1**: "데이터에 명시적으로 있는 사실만 사실이다"
2. **gaps-and-direction.md**: 없는 경험(AWS 운영, B2C 대규모 트래픽, OSS 기여 등)을 명시적으로 기술하고, 대신 "인접 경험 + 현재 방향성"으로 답하도록 유도
3. **temperature=0.2**: 창의성을 최소화하고 사실 기반 응답 유도

결과적으로 "모르면 모른다고 말하되, 관련 경험과 현재 학습 방향을 함께 제시"하는 정직한 답변 전략입니다.

### Q3. Gemini를 선택한 이유는? 왜 OpenAI가 아닌가?

**A.**
- **비용**: Gemini 2.5 Flash는 무료 티어 제공 — 포트폴리오 사이트에 월 과금 부담 없음
- **Spring AI 호환**: Google이 OpenAI API 호환 엔드포인트를 제공하므로, Spring AI의 OpenAI 스타터를 그대로 사용하면서 base-url만 변경
- **전환 용이**: 나중에 GPT-4o나 Claude로 바꾸려면 `application.properties`의 3줄만 수정

### Q4. Rate Limiting은 어떻게 구현했나요?

**A.** `SimpleRateLimiter` — 인메모리 Fixed Window 구현
- TCP peer 주소당 1시간 윈도우에서 20회 제한
- `ConcurrentHashMap<String, Bucket>`으로 peer별 상태 관리
- 윈도우 리셋: `now - windowStart >= 3600000ms`이면 카운터 초기화
- 호출자가 위조할 수 있는 `X-Forwarded-For`는 사용하지 않고 서버가 확인한 TCP peer 주소를 key로 사용
- 1시간 이상 접근하지 않은 bucket은 256회 요청마다 정리해 map이 계속 증가하지 않도록 관리

**왜 Redis 기반이 아닌가?**
- 단일 인스턴스 배포이므로 인메모리로 충분
- Redis 기반은 다중 인스턴스 시 필요 — 현재 규모에서는 오버엔지니어링

**현재 trade-off는?**
- Cloudflare 뒤에서는 여러 사용자가 같은 peer 주소 한도를 공유할 수 있음
- origin 인바운드를 Cloudflare 대역으로 제한한 사실을 검증한 뒤에만 `CF-Connecting-IP` 기반 사용자별 제한으로 확장

---

## 2. 아키텍처 & 패키지 구조

### Q5. 클린 아키텍처를 적용한 이유와 구조는?

**A.** 4개 레이어로 분리합니다:
```
domain/        → 순수 비즈니스 모델 + 포트 인터페이스
application/   → 유스케이스 / 서비스
presentation/  → 컨트롤러 (인바운드 어댑터)
infra/         → 설정, 외부 연동, 공통 유틸 (아웃바운드 어댑터)
```

**의존성 방향**: `presentation → application → domain ← infra`
- domain은 어떤 프레임워크에도 의존하지 않는 포트 인터페이스(`FileStorage`, `OAuthVerifier`, `TokenProvider`)를 정의
- infra에서 구현체(`S3FileStorage`, `GoogleOAuthVerifier`, `JwtTokenProvider`)를 제공
- 인프라 교체(S3→GCS, Google→카카오) 시 domain/application은 변경 없음

### Q6. 포트 인터페이스를 분리한 실질적 효과는?

**A.**
- `FileStorage` 포트: 현재 S3 구현, 테스트 시 InMemory 구현으로 교체 가능
- `OAuthVerifier` 포트: Google OAuth → 카카오/네이버로 확장 시 application 레이어 무변경
- `TokenProvider` 포트: JWT → 세션 기반으로 전환해도 비즈니스 로직 무영향

---

## 3. 인증 (Google OAuth + JWT)

### Q7. 인증 흐름을 설명해 주세요.

**A.**
```
1. 프론트에서 Google Sign-In → ID Token 발급
2. POST /api/auth/google { credential }
3. AuthService: GoogleOAuthVerifier로 ID Token 검증
4. 이메일이 마스터(BLOG_MASTER_EMAIL)와 일치하면 JWT 발급
5. JWT는 HttpOnly·Secure·SameSite=Strict 쿠키로 전달해 브라우저 JavaScript 접근 차단
6. 상태 변경 요청은 별도 CSRF 쿠키 값을 X-CSRF-Token 헤더에 담아 이중 검증
7. JwtAuthInterceptor가 /api/blog 쓰기 경로를 검증하고, 배포 스크립트의 환경변수 Bearer 토큰은 별도 경로로 지원
```

### Q8. Spring Security를 안 쓴 이유는?

**A.**
- 인증이 필요한 엔드포인트가 블로그 CRUD 4개뿐
- 현재는 인증 경로가 블로그 관리로 한정돼 `HandlerInterceptor`에서 쿠키/Bearer 인증과 CSRF 검증을 명시적으로 관리
- 단순히 “Spring Security가 과하다”가 아니라 보호 경로·세션 수명·권한 종류가 늘어나는 시점을 전환 기준으로 둠
- 관리자 기능이나 역할이 늘면 SecurityFilterChain, SecurityContext, 표준 CSRF 저장소로 전환해 누락 위험을 줄여야 함

---

## 4. 블로그 시스템

### Q9. S3 이미지 업로드 구조는?

**A.**
- `FileStorage` 포트 인터페이스 → `S3FileStorage` 구현
- 업로드 시 `blog-images/{UUID}.{확장자}` 키로 S3에 저장
- 버킷 정책으로 퍼블릭 읽기 허용 → CDN 없이 직접 S3 URL로 서빙
- IAM 사용자에 `PutObject`, `DeleteObject`만 허용 (최소 권한 원칙)
- S3 키 미설정 시 기동은 되지만 업로드 비활성화 (graceful degradation)

### Q10. 마크다운 렌더링은 어떻게 처리하나요?

**A.**
- 서버 사이드: `commonmark` 라이브러리 + GFM 테이블 확장
- 블로그 본문은 마크다운으로 저장, API 응답 시 HTML로 변환
- devlog/ops 페이지도 동일한 `MarkdownService`를 공유 — infra/common에 배치

---

## 5. CI/CD & 인프라

### Q11. CI/CD 파이프라인 구조는?

**A.**
```
main push → CI (빌드+테스트) → 성공 시 Deploy 자동 트리거
                                    ↓
                              SSH → EC2
                              git pull
                              docker compose build --no-cache
                              docker compose up -d
                              헬스체크 (30회 × 2초 = 최대 60초)
                                    ↓
                              실패 시 이전 커밋으로 롤백
```

### Q12. 롤백 전략은?

**A.** 배포 스크립트에서 자동 롤백:
1. 배포 전 `git rev-parse HEAD`로 현재 커밋 저장
2. `docker compose up -d` 후 `/actuator/health` 헬스체크
3. 60초 내 healthy 응답 없으면:
   - 로그 출력 (`docker compose logs --tail=50`)
   - `git checkout $PREV_COMMIT`
   - 이전 버전으로 재빌드 + 재기동
   - exit 1로 워크플로우 실패 처리

### Q13. Docker 구성은?

**A.** `docker-compose.yml`에 7개 서비스:
- **app**: Spring Boot (multi-stage build, JDK 21)
- **postgres**: 블로그 데이터 저장
- **redis**: 캐시 (10만건 상품 조회 데모)
- **kafka**: 이벤트 스트리밍 데모 (3 파티션)
- **actuator + micrometer**: 운영 health check와 애플리케이션 계측 유지
- **prometheus + grafana**: 로컬 `monitoring` 프로필에서 검증. t4g.small 운영에서는 핵심 서비스 자원 확보를 위해 제외
- profile `prod`로 운영 앱을 기동하고, `monitoring`은 로컬에서만 선택 실행

---

## 6. 데모 기능

### Q14. Redis 캐시 데모에서 10만건 시딩은 어떻게?

**A.**
- `DataSeeder`가 앱 기동 시 `product` 테이블 카운트 확인
- 목표 건수(10만) 미만이면 `saveAll(batch)`로 배치 삽입
- `@Cacheable`로 조회 캐싱, `@CacheEvict`로 무효화 데모
- 카테고리별 통계(`CategoryStats`) 집계 API 제공
- 공개 실행은 연결 peer 기준 분당 누적 400회, 전체 캐시 초기화 분당 10회로 제한

### Q15. Kafka 데모 구조는?

**A.**
- 3파티션 토픽 `livelab.orders`
- `OrderProducer`: 주문 이벤트 발행
- `OrderConsumer`: 소비 + 실패 시 DLT(Dead Letter Topic) 라우팅
- `KafkaMetricsService`: 소비/실패 카운트 실시간 메트릭
- 프론트에서 주문 발행 → 소비 결과를 실시간 확인하는 인터랙티브 데모
- 요청당 최대 2,000건, 연결 peer 기준 10분 누적 5,000건으로 제한해 공개 인프라의 자원 남용 방지
- 현재 제한 상태는 단일 JVM 메모리에 있으므로 수평 확장 시 Redis 기반 원자 카운터 등으로 전환 필요

---

## 7. 기술 선택 & 트레이드오프

### Q16. 프론트엔드를 Vanilla JS로 유지한 이유는?

**A.**
- 포지션이 백엔드 엔지니어 — React/Vue 투자는 포트폴리오 메시지 희석
- 번들러(webpack/vite) 불필요 → 빌드 파이프라인 단순화
- 정적 파일 서빙으로 CDN 친화적, 추가 인프라 비용 없음
- 필요한 인터랙션(fade-in, 카드 토글)은 Vanilla JS로 충분

### Q17. AWS 비용 최적화는 어떻게?

**A.**
- **EC2**: t4g.small (ARM) — 프리티어 또는 최소 비용
- **S3**: 퍼블릭 읽기만 허용, CloudFront 미사용 (트래픽 적음)
- **Gemini API**: 무료 티어 활용
- **탄력적 IP**: EC2에 연결 시 무료
- **Docker Compose**: 단일 인스턴스에 모든 서비스 — ECS/EKS 비용 회피

### Q18. 이 프로젝트에서 가장 어려웠던 점은?

**A.** (개인 경험에 맞게 수정하세요)
- CI/CD 파이프라인에서 SSH 연결, 보안 그룹, 환경변수 관리 등 인프라 레벨 트러블슈팅
- 클린 아키텍처 적용 시 패키지 간 의존성 방향 유지와 실용성 사이의 균형
- RAG 챗봇의 할루시네이션 방지를 위한 프롬프트 엔지니어링 반복

---

## 8. 확장성 & 개선점

### Q19. 트래픽이 늘어나면 어떻게 대응하나요?

**A.**
- **Rate Limiter**: 현재 인메모리 → Redis 기반으로 전환 (다중 인스턴스 대응)
- **RAG**: 데이터가 늘어나면 벡터 DB(pgvector) + 시맨틱 검색으로 전환
- **배포**: Docker Compose → ECS Fargate 또는 Kubernetes
- **캐시**: Redis Cluster 구성
- **CDN**: CloudFront 추가로 정적 자원 응답 속도 개선

### Q20. 현재 아키텍처의 한계를 알고 있나요?

**A.**
- **단일 인스턴스**: EC2 장애 시 전체 서비스 중단 → ALB + Auto Scaling Group 필요
- **정적 RAG**: 데이터 변경 시 앱 재시작 필요 → 핫 리로드 또는 DB 기반으로 개선 가능
- **모니터링**: t4g.small 자원 제약으로 Prometheus + Grafana를 운영 배포에서 제외해 시계열·Consumer Lag·캐시 히트율 상시 관측이 없음. 인스턴스 상향 또는 외부 관측 환경 분리가 필요
- **테스트**: 통합 테스트가 외부 인프라(Kafka, Redis) 의존 → Testcontainers 도입 필요
