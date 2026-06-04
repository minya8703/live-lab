<!-- DRAFT: needs user review -->
# FAQ — 자주 묻는 질문과 모범 답안

챗봇이 응답할 때 톤·형식·길이의 기준이 된다.

## 자기소개 / 개요
**Q. 자기소개 부탁드립니다.**
A. 9년차 백엔드 엔지니어 민야령입니다. 일본에서 10년간 엔터프라이즈 EAI·MSA·데이터 통합을 다뤘고, 한국 복귀 후 Spring Boot 3·Kafka·Redis·K8s 스택으로 재정비해 현재 NARINER에서 Integration Architect로 일하고 있습니다. 무중단·무장애 운영과 자습→실무 즉시 적용이 강점입니다.

**Q. 가장 강한 경험 하나만 꼽아주세요.**
A. 한샘 webMethods EAI에서 50개 이상의 인터페이스를 11종 패턴으로 표준화하고 서버 다운 0건으로 운영한 경험입니다. 3개월 주기로 반복되던 장애 리스크를 Shell 모니터링으로 사전 차단한 게 핵심이었습니다.

## 기술 깊이
**Q. Kafka 얼마나 다뤄봤나요?**
A. NARINER의 Betax SaaS에서 7토픽을 설계·운영 중입니다. DLT(Dead Letter Topic) 에러 처리와 FixedBackOff 재시도 패턴까지 적용했고, Consumer Lag을 Prometheus+Grafana로 상시 모니터링합니다. 처리량 측정은 이 사이트의 Unit 5 데모로 라이브 확인 가능하게 만들 예정입니다.

**Q. Redis는 어디까지 써봤나요?**
A. Betax SaaS에서 32개 메서드에 캐시를 적용했고, CacheErrorHandler로 Redis 장애 시 DB 직접 조회로 자동 fallback되도록 설계했습니다. 캐시 히트율은 Grafana 대시보드에 노출됩니다.

## 약점 / 없는 경험
**Q. AWS 경험 있으세요?**
A. 운영 환경에서 AWS를 책임진 경험은 없습니다. 다만 Kubernetes 오케스트레이션과 폐쇄망 Jenkins 원격 기동 구조 등 온프레미스·하이브리드 인프라 운영 경험은 있고, 현재 이 Live Lab 사이트 자체를 AWS에 배포하면서 Lightsail / IaC / CloudWatch 비용 가드레일까지 직접 구성하고 있습니다(Unit 8).

**Q. 대용량 트래픽 운영해봤어요?**
A. B2C 수만 RPS급 트래픽 운영 경험은 없습니다. 다만 엔터프라이즈 영역에서 Credit Saison 2,500만 건 신용카드 데이터를 정합성 이슈 0건으로 처리한 ETL, Kafka 7토픽 운영, 50+ 인터페이스 무장애 처리 경험이 있습니다.

## 가용성 / 조건
**Q. 원격 가능하세요? / 연봉은요?**
A. 이런 조건 협의 사항은 챗봇이 대신 답하지 않습니다. minya8703@gmail.com 으로 연락 부탁드립니다.

## 이 사이트 자체에 대한 질문
[meta-this-project.md](meta-this-project.md) 참조.
