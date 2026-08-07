# SAP↔Betax MSA 통합 솔루션

- 기간: 2023.10 ~ 2026.02
- 소속: NARINER
- 도메인: 회계·세금계산서 (SAP ERP ↔ 자체 SaaS 통합)
- **시스템 관계**: SAP RFC와 [02 Betax SaaS](02-betax-saas.md)를 MSA로 연결하는 인프라 레이어 (JCo Server + 중계서버).

## 기술 스택
Spring Boot, SAP JCo Server, MSA, 독립 DB, 별도 배포 파이프라인

## 핵심 기여
- **JCo Server ↔ 중계서버 ↔ 회계솔루션(=02 Betax SaaS)** MSA 아키텍처 전체 설계
- SAP 직접 연동과 Betax가 하나의 중계서버를 공유하도록 표준화해 발행처 인터페이스 변경 지점을 단일화
- 서비스별 독립 DB, 독립 배포 파이프라인
- 서비스별 독립 포트, RESTful API JSON 통신으로 결합도 최소화
- Git repository 분리
- 협력업체 휴·폐업 조회 주기를 주 1회에서 일 1회로 단축

## 결과
- 처리 시간 **70% 단축** (5분 → 1.5분/건)
- 중복 발행 오류 **0건**
- 건당 과금 구조 제거
