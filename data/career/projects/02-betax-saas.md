# Betax — 회계·세무 SaaS

- 기간: 2023.10 ~ 2026.02 (NARINER 자체 개발)
- 소속: NARINER
- 도메인: 회계·세무 SaaS · 고객사 3곳 운영 (재직 시점)
- **시스템 관계**: [03 SAP↔Betax MSA 통합 솔루션](03-msa-tax.md)의 종단(SaaS 레이어). SAP과는 03의 JCo Server·중계서버를 거쳐 연동된다.

## 기술 스택
Spring Boot 3.x, Kafka (7토픽 · DLT · FixedBackOff), Redis (32메서드 캐시 · CacheErrorHandler), Kubernetes, Prometheus, Grafana

## 핵심 기여
- Kafka 7토픽을 업무 이벤트별로 분리하고, DLT(Dead Letter Topic) 에러 처리와 FixedBackOff 재시도 패턴 적용
- Redis Key-Value 캐시를 32개 메서드에 적용. 환율·분석키 TTL 24시간, 회사·거래처 마스터 TTL 12시간으로 차등 설계
- DB를 원본으로 유지하는 Cache-Aside와 @CacheEvict 무효화 적용, CacheErrorHandler로 Redis 장애 시 DB fallback 자동화
- Prometheus + Grafana 대시보드 (캐시 히트율, Kafka Consumer Lag)
- Kubernetes HPA, Rolling Update, Liveness/Readiness Probe 적용

## 결과
- 고객사 3곳 동시 운영
- 캐시·메시징·관측성을 갖춘 SaaS 아키텍처 안정 운영 (재직 시점 기준)
