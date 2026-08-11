# Meta — 이 사이트(Live Lab) 자체에 대한 답변 가이드

면접관이 "이 사이트는 어떻게 만든 거에요?" 같은 질문을 하면 이 문서 기반으로 답한다.

## 한 줄 정의
**민야령의 백엔드 라이브 랩** — Kafka·Redis·MSA를 제한된 환경에서 직접 다루고, 구현의 한계와 trade-off까지 AI-DLC 개발 일지로 공개하는 사이트.

## 왜 만들었나
기존 정적 포트폴리오(https://minya8703.github.io/)는 이력을 잘 정리했지만, "이 사람이 실제로 돌릴 수 있는가?"는 증명하지 않는다.
이 사이트는 그 격차를 메운다 — Redis·Kafka 데모는 실제 백엔드에서 실행되고, 현재 구현의 한계와 다음 개선 방향도 함께 공개한다.

## 어떻게 만들고 있나
**AI-DLC (AI-Driven Development Lifecycle)** 방식.
Inception → Construction → Operations 사이클로, 기능을 Unit 단위로 나눠 한 조각씩 끝까지 출시.

- U1: Inception 산출물 문서화 (완료)
- U2: 랜딩 페이지 (완료)
- U3: AI 경력 Q&A 챗봇 — 모델의 source·원문 line 위치 검증 후 `answer/sources/grounded` 응답 공개
- U4: Redis 캐시 라이브 데모
- U5: Kafka 처리량 데모
- U6: Prometheus·Grafana 로컬 검증 후 t4g.small 운영 배포 제외 — 자원 trade-off 회고 공개
- U7: AI-DLC 개발 일지 (상시)
- U8: AWS 배포 + 비용 가드레일
- U9: SEO · OG · Analytics · 보안 헤더

## 기술 스택
- 백엔드: Spring Boot 3.4, Java 21, Spring AI 1.0
- LLM: Google Gemini 2.5 Flash via OpenAI 호환 엔드포인트
  - 의도: Spring AI의 프로바이더 추상화 활용 + 무료 티어
  - swap: Claude → Gemini 마이그레이션을 Java 코드 변경 0줄로 진행 (의존성·properties만 교체)
- 프론트: Vanilla JS + HTML/CSS (의도적 단순화 — 백엔드 스포트라이트)
- 데이터: 이 디렉토리(`data/career/`)의 마크다운 파일 + 줄 번호를 부여한 전체 컨텍스트 주입 + 응답 source·line 위치 검증
- 인프라: AWS EC2 t4g.small + Docker Compose + Cloudflare, AWS Budget 월 $15 가드레일
- 운영 모니터링: Actuator·Micrometer 계측 유지. Prometheus·Grafana는 자원 제약으로 AWS 운영에서 제외하고 로컬 `monitoring` 프로필로 분리

## 의도적 선택과 이유
- **RAG 임베딩 안 씀**: 경력 데이터가 ~12K 토큰으로 작아 컨텍스트 주입 + 프롬프트 캐싱이 더 단순하고 저렴.
- **프론트 프레임워크 안 씀**: 백엔드 어필이 목적이라 진열대(프론트)를 더 단단하게 만드는 데 자원 안 쓴다.
- **챗봇 모델은 Gemini 2.5 Flash**: 무료 티어 + 경력 Q&A 정확도엔 충분.
- **근거 응답은 fail-closed**: JSON 형식 오류, 빈 근거, 존재하지 않는 source ID, 범위를 벗어나거나 빈 line은 모델 답변을 노출하지 않고 보류 응답으로 전환. 선택한 원문 줄이 답변의 모든 주장을 의미적으로 뒷받침하는지 판정하는 평가는 별도 단계.
- **프롬프트 공격은 입력·출력 이중 방어**: 명시적인 지시 무시·내부 프롬프트 탈취·허위 경력 생성 요청은 LLM 호출 전에 차단하고, 내부 규칙 조각이 출력되면 폐기. 정규식 우회 가능성과 실제 모델 품질 평가는 남은 한계.

## 면접관이 자주 묻는 메타 질문 예시
- "Spring AI 처음 써본 거에요?" → 그렇다. 이 사이트가 첫 실습. 학습 과정은 [AI-DLC 일지](U7 페이지)에 공개.
- "왜 Vanilla JS에요?" → 백엔드 포지션 어필 목적. 프레임워크 학습 시간을 백엔드에 썼다.
- "AWS 비용은 어떻게 관리해요?" → AWS Budget 월 $15 가드레일과 50%·90%·예측 100% 알람으로 관리.
- "RAG는 왜 안 썼어요?" → 데이터가 작아 오버엔지니어링. 단순 컨텍스트 주입이 더 단순·저렴.
- "왜 Claude가 아니라 Gemini에요?" → 초기 설계는 Anthropic Claude였다. Construction 중 비용·무료 티어 검토 후 Google Gemini로 swap. Spring AI 추상화 덕분에 의존성·properties만 교체, Java 코드 변경 0줄. Spring AI를 선택한 실질적 이유가 이 swap에서 입증됨.
