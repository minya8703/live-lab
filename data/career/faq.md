# FAQ — 자주 묻는 질문과 모범 답안

챗봇이 응답할 때 톤·형식·길이의 기준이 된다.

## 자기소개 / 개요

**Q. 자기소개 부탁드립니다.**
A. 9년 6개월 경력 Backend / EAI Engineer 민야령입니다. 일본 SI에서 6년 6개월간 금융 결제(Credit Saison·MONEX), 항공 발권(ANA), 전력 청구(규슈전력) 등 데이터 정합성과 무결성이 중요한 시스템을 담당했고, 한국에서는 한샘 EAI 운영과 자체 SaaS BeTAX 백엔드 + SAP↔BeTAX MSA 통합 솔루션을 Spring Boot 3·Kafka·Redis·Kubernetes로 설계·구축했습니다. 무중단·무장애 운영과 자습→실무 즉시 적용이 강점입니다.

**Q. 가장 강한 경험 하나만 꼽아주세요.**
A. 한샘 webMethods EAI에서 50개 이상의 인터페이스를 11종 패턴으로 표준화하고 재직 기간 서버 다운 0건으로 운영한 경험입니다. 3개월 주기로 반복되던 장애 리스크를 Shell 기반 JVM 메모리 모니터링(80% 임계값 알람)으로 사전 차단한 게 핵심이었습니다.

## 기술 깊이

**Q. Kafka 얼마나 다뤄봤나요?**
A. NarinER 자체 SaaS BeTAX(SAP과의 MSA 통합 솔루션의 SaaS 레이어)에서 7개 토픽(journal-events, budget-alerts, tax-invoice-requests 등)을 설계·운영했습니다. DLT(Dead Letter Topic) 에러 처리와 FixedBackOff 재시도 패턴까지 적용했고, Consumer Lag을 Prometheus + Grafana로 상시 모니터링했습니다. 처리량 측정은 이 사이트의 Unit 5 데모로 라이브 확인 가능하게 만들어 공개했습니다.

**Q. Redis는 어디까지 써봤나요?**
A. BeTAX SaaS에서 32개 메서드에 캐시를 적용했습니다. 환율 데이터 TTL 24시간, 회사·거래처 마스터 TTL 12시간 등 도메인별 TTL을 차등 설계했고, CacheErrorHandler로 Redis 장애 시 DB 직접 조회로 자동 fallback되도록 구성했습니다. 캐시 히트율은 Grafana 대시보드로 가시화했습니다.

## 운영 / 클라우드

**Q. AWS 경험 있으세요?**
A. 기업 운영 환경에서 AWS를 책임진 경험은 없습니다. 다만 이 라이브 사이트(minya.life)를 AWS EC2(t4g.small, Seoul) + Docker Compose 4컨테이너(Spring·Postgres·Redis·Kafka) + Cloudflare Proxy로 직접 배포·운영하고 있고, AWS Budget $15/mo로 비용 가드레일을 3단 알람(50%·90%·예측 100%)으로 등록해 운영 중입니다. 카드 만료로 인스턴스가 자동 terminate된 장애도 60분 내 복구해 그 회고를 사이트에 공개했습니다.

이전 경력에서는 Kubernetes 오케스트레이션(HPA, Rolling Update, Liveness/Readiness Probe)과 폐쇄망 환경 Jenkins 원격 기동 구조 등 온프레미스·하이브리드 인프라 운영 경험이 있습니다.

**Q. 대용량 트래픽 운영해봤어요?**
A. B2C 수만 RPS급 트래픽 운영 경험은 없습니다. 다만 엔터프라이즈 영역에서 회원 약 2,500만 규모의 Credit Saison 신용카드 데이터를 담당 범위 내 정합성 이슈 0건으로 통합한 경험, BeTAX의 Kafka 7개 토픽 비동기 처리, 한샘 EAI 50개 이상 인터페이스를 재직 기간 서버 다운 0건으로 운영한 경험이 있습니다.

## 가용성 / 조건

**Q. 원격 가능하세요? / 연봉은요?**
A. 협의 사항은 직접 답변드리는 것이 정확할 것 같습니다. minya8703@gmail.com 으로 연락 부탁드립니다.

## 이 사이트 / 챗봇 자체에 대한 질문

**Q. 이 챗봇은 어떻게 만들어졌나요?**
A. Spring Boot 3 + Spring AI + Google Gemini 2.5 Flash 조합으로 구현했습니다. 이력서 데이터와 FAQ 마크다운을 컨텍스트로 주입해 답변을 생성하고, 없는 경험은 "없습니다"로 솔직히 답하도록 설계했습니다. AWS EC2 + Docker Compose + Cloudflare 환경에서 운영 중이며 월 비용 $15 가드레일 안에서 동작합니다. 상세는 [meta-this-project.md](meta-this-project.md) 참조.
