# Inception — 민야령 Backend Live Lab

**Version:** v1.0
**Date:** 2026-05-21
**Methodology:** AI-DLC (Inception → Construction → Operations)
**Status:** Approved (2026-05-21)

---

## 1. Vision

> **"민야령이 9년간 다뤄온 Kafka·Redis·MSA를 라이브로 증명하고, 그것을 AI-DLC로 만들어가는 과정 자체를 콘텐츠로 노출하는 백엔드 라이브 랩(Live Lab)."**

기존 사이트(<https://minya8703.github.io/>)가 "이력서"라면, 이 사이트는 **"열려있는 작업실"**.

- 기존 사이트는 그대로 유지·보완 관계. 대체 아님.
- 이 사이트는 라이브 데모 + 개발 과정 자체를 콘텐츠로 노출하는 포지션.

---

## 2. Personas

우선순위 순.

| # | 페르소나 | 첫 30초에 던지는 질문 | 우리가 줘야 할 답 |
|---|---|---|---|
| **P1** | 리크루터 / HR | "이 사람 서류 넘길 만한가?" | 5초 안에 명확한 1줄 포지셔닝 + 3개 수치 |
| **P2** | 현업 시니어 백엔드 | "Kafka·Redis 정말 만져봤나?" | 클릭하면 돌아가는 라이브 데모 + 코드 링크 |
| **P3** | 테크 리드 / 아키텍트 | "설계 의사결정은 어떻게 하나?" | ADR(Architecture Decision Record), AI-DLC 일지 |

P1을 통과시키지 못하면 P2·P3까지 갈 일이 없으므로 **P1 최우선**.

---

## 3. Success Metrics

| 지표 | 측정 방법 | 목표 |
|---|---|---|
| 서류 통과율 (본질 KPI) | 사용자가 직접 트래킹 | 적용 전 대비 상승 |
| 평균 체류 시간 | GA4 / Plausible | 90초 이상 |
| 챗봇 질문 수 / 방문 | 자체 로깅 | 방문당 ≥1회 |
| 면접 시 "사이트 봤다" 언급 | 본인 기록 | 면접 중 30% 이상 |
| 월 인프라 비용 | AWS 빌링 | **$15 이하** |

---

## 4. Differentiators (vs 기존 사이트)

| 영역 | 기존 사이트 | 이 사이트 |
|---|---|---|
| 형식 | 정적 텍스트 카드 | 실행되는 데모 + 실시간 그래프 |
| Kafka / Redis | 수치 텍스트 ("7토픽", "32메서드") | 클릭 한 번에 실제 실행 |
| AI | 언급 없음 | RAG 챗봇 + AI-DLC 개발 일지 |
| 클라우드 | 언급 없음 | AWS 배포·관측·비용 노출 |
| 개발 과정 | 없음 | "이 사이트는 AI-DLC로 만들어졌습니다" 메타 페이지 |

---

## 5. Non-Goals (스코프 잠금)

- 실제 사용자 트래픽 처리 (1인 데모용)
- 99.9% SLA / 멀티 AZ / Auto-Scaling
- 풀 디자인 시스템 / 다크모드 토글 등 부가 기능
- Claude 외 모델 다중 지원
- 회원가입 · 로그인 (관리자 외)
- **프론트엔드 프레임워크 도입 (React/Next.js/Vue 등)** — 백엔드 스포트라이트 유지를 위해 Vanilla JS + HTML/CSS로 제한
- 빌드 체인·번들러 도입 (백엔드 어필과 무관)

---

## 6. Units of Work (Construction 백로그)

**원칙**: 각 Unit은 "설계 → 코드 → 배포 → 공개 가능 상태"까지 한 번에. 다음 Unit은 이전 Unit이 실제로 외부에서 보이는 상태가 된 뒤 시작.

| # | Unit | 산출 가치 | 의존 |
|---|---|---|---|
| **U1** | Inception 산출물 문서화 (`docs/inception.md`) | 합의의 단일 진실 | — |
| **U2** | 랜딩 페이지 + 기존 사이트 링크 + 도메인 결정 | 외부 공개 시작점 | U1 |
| **U3** | AI 경력 Q&A 챗봇 (Claude API + RAG) | **P1·P2 동시 어필의 최강 단일 기능** | U2 |
| **U4** | Redis 캐시 라이브 데모 (실시간 실행) | 가벼운 첫 데모 | U2 |
| **U5** | Kafka 처리량 데모 (사전 측정 결과 + 재실행) | 대용량 어필 | U4 |
| **U6** | Grafana 공개 대시보드 + Prometheus 메트릭 | 관측 어필 | U5 |
| **U7** | AI-DLC 개발 일지 페이지 (이 사이트 자체의 메타) | 차별점의 핵심 콘텐츠 | U2~U6 진행 중 누적 |
| **U11** | Test Automation — Testcontainers 통합 테스트 + GitHub Actions CI | 백엔드/테스트 포지셔닝 공백 보완. 진짜 인프라로 검증 + 자동 실행 | U7 |
| **U8** | AWS 배포 자동화(IaC) + 비용 가드레일 | 운영 어필, Operations 단계 진입 | U11 |
| **U10** | AWS 개발/운영/배포 페이지 (`/lab/ops`) | "혼자 운영해본 경험" 콘텐츠화 — CI/CD 런북 + 장애 플레이북 | U8 |
| **U9** | SEO / OG / Analytics + 보안 헤더 | 서류 통과율 KPI 측정 가능 상태 | U10 |

**우선순위**: U1 → U2 → **U3** → U4 → U5 → U6 → U7(상시) → **U11** → U8 → **U10** → U9.
U3가 끝나는 시점에 처음으로 외부에 보여줄 수 있는 단단한 **V0**이 나옴.
U11이 U8 앞에 오는 이유 — AWS 배포 전에 통합 테스트가 갖춰져야 *"테스트 없는 코드를 배포한다"* 라는 안티 패턴을 피함.

---

## 7. 아키텍처 선택지 (Construction 진입 직전 결정 — 지금은 잠그지 않음)

| 영역 | 후보 | 결정 시점 |
|---|---|---|
| 호스팅 | Lightsail $10 / EC2 Free Tier / Fly.io / Render | U3 진입 직전 |
| 프론트 | **Vanilla JS + HTML/CSS (확정)** — Spring Boot 정적 서빙 vs 별도 정적 호스팅 | U2 진입 직전 |
| 챗봇 백엔드 | Spring Boot + Spring AI (유력) vs 별도 FastAPI | U3 진입 직전 |
| DB | SQLite(로컬) → Postgres on Lightsail / Supabase 무료 | 데이터 모델 잡힐 때 |
| 도메인 | 신규 구매 / 기존 보유 / IP 시작 | U2 진입 직전 |

---

## 8. Decision Log

| 일자 | 결정 | 근거 |
|---|---|---|
| 2026-05-21 | AI-DLC 방식 채택 | 개발 과정 자체가 차별 콘텐츠 (사용자 요구) |
| 2026-05-21 | 기존 사이트는 유지·보완 관계 | 중복 콘텐츠 방지, 디자인 톤 일관성 |
| 2026-05-21 | P1(리크루터) 최우선 페르소나 | 본질 KPI가 "서류 통과" |
| 2026-05-21 | 월 비용 상한 $15 | 명시적 비용 제약 |
| 2026-05-21 | Inception v1.0 승인 | 사용자 확인 — Open Questions는 각 Unit 진입 시점에 컨텍스트로 결정 |
| 2026-05-21 | 프론트는 Vanilla JS + HTML/CSS로 확정 (프레임워크 도입 금지) | 백엔드/아키텍처/테스트 포지션 목표. 프론트는 어필 대상이 아닌 진열대. 사용자에게 React/JSP/JS 경험은 이미 충분. |
| 2026-05-21 | 도메인은 사용자 직접 구매 (이름 미정, U8 전에 공유) | 사용자 결정 |
| 2026-05-21 | 디자인 톤: 기존 사이트 흑백 미니멀 + 포인트 컬러 Emerald #10B981 | 기존 사이트와 브랜드 일관성. Emerald는 Healthy/Live/Monitoring 함의 — 라이브 랩 컨셉과 정합 |
| 2026-05-21 | 정적 파일은 `src/main/resources/static/`에 두기 (Spring Boot 기본 경로) | U3에서 Spring Boot 도입 시 파일 이동 불필요. 그전에는 브라우저 직접 오픈 또는 간단 HTTP 서버로 프리뷰 |
| 2026-05-21 | U3 LLM 프로바이더: Anthropic Claude → Google Gemini 2.5 Flash 로 변경 | 무료 티어로 월 비용 $15 가드레일 더 여유. Spring AI 추상화로 의존성·properties만 교체(Java 코드 변경 0줄). OpenAI 호환 엔드포인트(`generativelanguage.googleapis.com/v1beta/openai/`) 사용. 이 swap 자체가 AI-DLC 일지(U7) 콘텐츠로 활용. |
| 2026-05-21 | 비밀 관리는 `.env` + `spring-dotenv` 라이브러리로 일원화 | 사용자 선호. `.env.example` 트래킹 + `.env` gitignore. `application-local.properties` 패턴은 보조로만 유지. 사용자가 U3 검증 중 `application.properties`에 키 평문 입력했다가 즉시 패턴 복구한 사건이 계기 → AI-DLC 일지(U7)에 회고 항목으로 기록. |
| 2026-05-21 | Postgres 호스트 포트 5432 → 5433 변경 | U4 검증 중 호스트의 다른 Postgres가 5432 점유 → Spring이 잘못된 Postgres에 연결되어 인증 실패로 1시간+ 디버깅. 진단 교훈: "auth failed"가 실제로는 "엉뚱한 서비스에 연결" 신호일 수 있음. AI-DLC 일지(U7)에 가치 있는 회고 사례. |
| 2026-05-21 | U10 추가 — AWS 개발/운영/배포 페이지 | 사용자 요청. 기존 사이트의 가장 큰 공백(클라우드/운영)을 단순 배포가 아닌 콘텐츠로 노출. CI/CD 런북 + 장애 플레이북 2개 섹션 구성 — P2 시니어·P3 아키텍트의 "혼자 운영해봤나" 질문 정면 답변용. U8 완료 후 진입, U9는 U10에 의존. |
| 2026-05-24 | U11 추가 — Test Automation (Testcontainers + GitHub Actions CI) | 사용자 요청. 자동 테스트 부재가 그동안의 디버깅 사고(포트 충돌·Postgres 미기동 등) 절반의 원인. 백엔드/테스트 포지셔닝에서 가장 큰 공백. U7 마감 직후 진입, U8(AWS 배포) 전에 완료해야 함 — 테스트 없는 코드 배포 회피. |
| 2026-05-24 | U8 호스팅 전략: **AWS Phase 1 단독** (6개월 크레딧 $100~200 활용) | AWS Free Tier 정책 변경(2025-07-15) 반영. EC2/Lightsail + S3 + CloudFront + Lambda + CloudWatch 직접 사용으로 채용 공고의 AWS 키워드 직접 충족. 6개월 후 Phase 2(NAS 이전 또는 다른 옵션)는 별도 결정. |
| 2026-05-24 | NAS(DS725+) Phase 2 옵션은 현재 보류, 추후 재검토 | 사용자 결정. AWS 6개월 운영 후 시점에 NAS·Oracle·AWS 유지 중 선택. 현재는 단순화 우선. |
| 2026-05-24 | 도메인: **minya.life** 확정 (한국 레지스트라 첫 해 3,000원 프로모션) | 사용자 결정. `.life`가 "기술 인생 기록" Knowledge Garden 컨셉과 정합. 단, 갱신 시 연 25,000~35,000원으로 점프 — 11개월 시점에 Cloudflare Registrar(도매 약 13,500원/년)로 이전 권장. 캘린더 알림 필요. |
| 2026-06-04 | U8 완료 — https://minya.life 외부 노출 + Budget 알람 등록 | EC2 t4g.small (Sydney ap-southeast-2) + Docker Compose (Spring·Postgres·Redis·Kafka 5컨테이너) + Cloudflare Proxy (Flexible SSL) + AWS Budget $15/mo 50%·90%·예측 100% 3단 알람. 챗봇이 라이브에서 "AWS 운영 중" 답변 정상 — gaps-and-direction.md 의 핵심 자산 활성화. Phase 2(NAS 이전 등)는 별도 결정. |
| 2026-06-04 | U8 디버깅에서 회수할 회고 8건 (U10 콘텐츠 + 별도 devlog) | (1) SSH key 권한 함정: 이름이 같은 사용자/머신(minya/MINYA) 에서 icacls 가 머신 SID 로 grant. SID 직접 지정으로 해결. (2) Cloudflare nameserver 한 제공자로 통일 규칙. (3) Cloudflare SSL Full 모드 → 521. Flexible 로 변경. (4) Docker buildx 0.17+ 별도 설치. (5) EC2 포트 80 매핑은 .env 의 APP_HOST_PORT 로 외부화. (6) AWS Free Tier 변경(2025-07): 12개월 무료 → 6개월 크레딧. (7) SSH 키는 로컬 생성 + AWS 에 Import (AWS-side 생성 시 .pem 분실 위험). (8) 보안 그룹 Source 가 My IP 면 Cloudflare 차단 — 0.0.0.0/0 으로. |
| 2026-06-05 | 카드 만료 → EC2 자동 terminate 사건 + 재배포 60분 복구 + EIP 영구 적용 | 6개월 크레딧 모드 비용 $0 였는데 등록 카드 만료로 grace 7~14일 후 인스턴스 자동 terminated. 결정적 단서는 Billing 콘솔의 "권장 조치(3)" 빨간 배너 "만료된 결제 방법". 코드/회고 모두 git 자산이라 데이터 손실 0. 재배포 중 회고 #01(SSH SID) + #04(buildx) 가 새 환경에서 완전 재현 — 회고 따라 두 함정 합쳐 약 2시간 디버깅을 15분으로 단축 (ROI 약 3.8배). 회고 카드 3장 추가: 05-aws-payment-method-trap, 06-elastic-ip-2024-pricing, 07-incident-cards-validated-on-rebuild. user-data.sh 의 4.5 단계에 buildx v0.17.1 명시 설치 영구 추가 → 3차 셋업부터는 함정 자체 소멸. 재발 방지: EIP 영구 attach, 카드 만료 90일 전 알람, AMI 스냅샷 주기 권장. |

---

## 9. Open Questions (각 Unit 진입 시점에 결정)

승인 시점에는 별도 답변 없이 통과. 각 Unit 진입 직전 컨텍스트에서 결정한다.

1. 월 비용 상한 — U8(AWS 배포) 진입 직전 재확인.
2. Non-Goals 추가 — 매 Unit 종료 시 점검.
3. U3 vs U4 우선순위 — U2 종료 시점 재확인.
4. AI-DLC 일지 공개 범위 — U7 진입 직전 결정.
5. 디자인 톤 — U2 진입 직전 결정.

---

*다음 단계*: U2 진입. 프론트 프레임워크 / 도메인 / 디자인 톤 결정 필요.
