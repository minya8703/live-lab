---
slug: aws-phase1-deployment
title: AWS Phase 1 배포 — 따라하기 가능한 런북 요약
date: 2026-06-04
tags: [aws, ec2, cloudflare, runbook]
---

## 한 줄

도메인 + EC2 + Docker + Cloudflare Proxy 로 사이트를 외부 노출, 월 비용 $0 (6개월 AWS 크레딧 + Cloudflare Free).

## 아키텍처

```
[Browser]
   ↓ HTTPS
[Cloudflare Edge]            ← Universal SSL, CDN, DDoS
   ↓ HTTPS (Full strict 목표)
[EC2 t4g.small ARM]          ← Sydney ap-southeast-2
   ↓ Docker Compose
┌─ Spring Boot (port 80, 컨테이너 내부 8089)
├─ Postgres 16
├─ Redis 7
└─ Kafka 3.7 KRaft
```

## 6단계 요약

1. **도메인 + Cloudflare** (15분 + 전파 1~24h) — minya.life 구매 → Cloudflare 가입 → ns 변경
2. **AWS 계정 + IAM** (45분) — root MFA, 일상 작업용 IAM 주체 분리, 배포에 필요한 최소 권한만 부여
3. **EC2 launch** (30분) — Amazon Linux 2023 (Arm), t4g.small, key pair Import (로컬 생성)
4. **앱 배포** (20분) — SSH, git clone, `.env` 작성, `docker compose --profile prod up -d`
5. **Cloudflare 연결** (15분) — A 레코드 2개 (Proxied), origin 인증서 구성, SSL/TLS **Full (strict)**, Always Use HTTPS
6. **비용 가드레일** (10분) — AWS Budget $15/mo + 50%/90%/예측 100% 이메일 알람

## 핵심 의사결정

| 결정 | 이유 |
|---|---|
| t4g.small (ARM) | Graviton 가격·전력 효율. 6개월 크레딧으로 무료 |
| Cloudflare Full (strict) | 사용자부터 origin까지 암호화하고 origin 인증서를 검증 |
| Docker port `80:8089` | 호스트 80 노출 → Cloudflare 가 표준 포트로 도달 |
| AWS Budget 50%·90%·예측 100% | 50% 는 조기 경보, 90% 는 위험, 예측 100% 는 추세 기반 |
| Origin 접근 제한 | HTTP(S)는 Cloudflare 프록시 대역만, SSH는 관리자 고정 IP만 허용하고 변경 시 검토 |

초기 장애 대응 과정에서는 임시로 넓은 권한과 `Flexible` SSL을 사용했다. 이는 재현 가능한 배포 절차나 권장 구성에 포함하지 않으며, 운영 전 최소 권한·origin 인증서·접근 제한을 검증한다.

## 풀 상세

- 단계별 명령어 + 스크린샷: [docs/u8-deployment-runbook.md](https://github.com/minya8703/live-lab/blob/main/docs/u8-deployment-runbook.md)
- EC2 user-data 자동화 스크립트: `infra/aws/user-data.sh`
- Budget JSON 정의: `infra/aws/budget.json`

## 디버깅 사례

이 런북을 처음 실행하면서 만난 함정들은 [장애 시나리오 + 대응 플레이북](#) 섹션에서 확인.
