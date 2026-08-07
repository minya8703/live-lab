---
slug: monitoring-resource-tradeoff
unit: 6
title: t4g.small에서 Prometheus·Grafana를 제거한 이유 — 관측성과 가용성의 우선순위
date: 2026-08-08
tags: [aws, monitoring, architecture, trade-off]
---

## 맥락

로컬에서는 Spring Boot Actuator·Micrometer 메트릭을 Prometheus가 수집하고,
Grafana에서 JVM·HTTP·캐시·Kafka 지표를 확인하는 구성을 만들었다.
기능이 동작했으므로 같은 구성을 AWS EC2에도 배포하려 했다.

운영 인스턴스는 **t4g.small — 2 vCPU, 2GiB 메모리**다.
이미 Spring Boot·Postgres·Redis·Kafka가 한 인스턴스의 자원을 공유하고 있었다.

## 관찰한 문제

Prometheus와 Grafana까지 함께 실행하자 인스턴스 전반의 응답이 느려졌다.
이 환경에서는 모니터링 자체가 핵심 서비스가 사용할 CPU·메모리와 경쟁했다.

정확한 개선율을 남길 만큼 제거 전후의 부하 테스트 데이터를 확보하지는 못했다.
따라서 성능 향상 수치를 만들지 않고, 배포 과정에서 관찰한 응답 저하와
2GiB라는 명확한 자원 한계를 의사결정 근거로 남긴다.

## 검토한 선택지

1. **그대로 운영** — 대시보드는 유지되지만, 관측 대상 서비스의 응답성을 희생한다.
2. **인스턴스 상향** — 가장 단순하지만 현재 포트폴리오 트래픽과 비용 기준에는 과하다.
3. **관측 환경 외부 분리** — 바람직하지만 별도 인프라와 운영 비용이 필요하다.
4. **운영에서 제거하고 로컬 선택 구성으로 유지** — 현재 제약 안에서 핵심 서비스의 가용성을 우선한다.

채택한 선택지는 4번이다.

## 결정

- AWS 운영 기본 구성에서는 Prometheus·Grafana 컨테이너를 실행하지 않는다.
- Docker Compose의 두 서비스를 `monitoring` 프로필로 분리해 로컬에서만 선택 실행한다.
- Spring Actuator·Micrometer 계측 코드는 유지해 관측 스택을 다시 붙일 수 있는 경계는 보존한다.
- 운영 배포의 자원은 Spring Boot·Postgres·Redis·Kafka에 우선 배분한다.

```bash
# 기본 개발 의존성
docker compose up -d

# 모니터링까지 검증할 때만
docker compose --profile monitoring up -d
```

## 감수한 trade-off

제거 결정으로 운영 시계열, Consumer Lag, 캐시 히트율을 Grafana에서 상시 확인할 수 없게 됐다.
이는 무료 최적화가 아니라 **관측 가능성을 낮추고 핵심 서비스의 가용성을 택한 교환**이다.

트래픽이나 장애 대응 요구가 커지면 현재 구성을 정답으로 고집하지 않는다.
인스턴스를 상향하거나 관측 스택을 별도 환경으로 분리한 뒤 Prometheus·Grafana를 재도입하는 것이 다음 단계다.

## 교훈

**모니터링은 많이 붙일수록 좋은 부가 기능이 아니라, 자원을 소비하는 운영 워크로드다.**

관측 도구 때문에 관측 대상이 느려진다면 아키텍처의 목적이 뒤집힌다.
중요한 것은 도구를 설치했다는 사실이 아니라, 현재 자원·트래픽·복구 요구에 맞춰
어디까지 관측하고 무엇을 포기할지 설명할 수 있는 것이다.
