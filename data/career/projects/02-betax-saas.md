<!-- DRAFT: needs user review -->
# Betax — 회계·세무 SaaS

- 기간: NARINER 자체 개발 (재직 시점 2023.06 ~ 2026.03)
- 소속: NARINER
- 도메인: 회계·세무 SaaS · 고객사 3곳 운영 (재직 시점)

## 기술 스택
Spring Boot 3.x, Kafka (7토픽 · DLT · FixedBackOff), Redis (32메서드 캐시 · CacheErrorHandler), Kubernetes, Prometheus, Grafana

## 핵심 기여
- Kafka 7토픽 설계, DLT(Dead Letter Topic) 에러 처리, FixedBackOff 재시도 패턴
- Redis 32개 메서드에 캐시 적용, CacheErrorHandler로 Redis 장애 시 DB fallback 자동화
- Prometheus + Grafana 대시보드 (캐시 히트율, Kafka Consumer Lag)
- K8s 오케스트레이션

## 결과
- 고객사 3곳 동시 운영
- 캐시·메시징·관측성을 갖춘 SaaS 아키텍처 안정 운영 (재직 시점 기준)
