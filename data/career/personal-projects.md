# 개인 프로젝트와 Google Cloud / AI 활용

## Backend Live Lab
- 기간: 2026.05 ~ 현재
- 공개 서비스: https://minya.life
- 저장소: https://github.com/minya8703/live-lab
- 역할: 개인 설계·개발·배포·운영
- Java 21, Spring Boot 3.4, Spring AI, PostgreSQL, Redis, Kafka로 구현했다.
- Google AI Studio의 Gemini 2.5 Flash API를 Spring AI의 OpenAI 호환 인터페이스로 연결했다.
- 초기 Claude 설계에서 Gemini로 바꿀 때 Spring AI 추상화 덕분에 Java 애플리케이션 코드 변경 없이 의존성과 설정만 교체했다.
- AI 경력 Q&A는 모델이 제시한 source와 원문 line 위치를 서버에서 검증하고, 근거가 유효하지 않으면 답변을 노출하지 않는 fail-closed 계약을 적용했다.
- AWS EC2 t4g.small, Docker Compose, Cloudflare로 공개 운영하며 AWS Budget 월 15달러 가드레일을 구성했다.
- Google 관련 경험의 정확한 범위는 Gemini API 통합·운영과 2026 Google Cloud AI Study Jam 실습 학습이다. 기업 환경에서 GCP 인프라를 책임진 경력으로 표현하지 않는다.
- Google Cloud 학습 이력은 Google Skills 공개 프로필(https://www.skills.google/public_profiles/58876272-e765-4d7b-8d60-b7fea3158491)에서 확인할 수 있다.
