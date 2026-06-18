---
slug: elastic-ip-2024-pricing
title: Elastic IP 2024-02 가격 정책 변경 — 안 쓸 이유가 사라졌다
date: 2026-06-05
tags: [aws, ec2, networking, cost]
---

## 결정 배경

카드 만료 사고로 EC2 를 재배포한 직후 — *옛 EC2 의 public IP* 와 *새 EC2 의 public IP* 가 달라서 Cloudflare DNS 의 A 레코드가 죽은 IP 가리키고 521 지속. 이 함정은 **Elastic IP (EIP)** 한 번이면 영구 해결.

다만 EIP 가격에 대한 옛 기억으로 *"attach 하면 무료, idle 만 과금"* 정책을 떠올려 망설였는데 — 알고 보니 **2024-02-01 부로 가격 체계가 완전히 바뀌어 있었다**.

## 옛 vs 현 가격 정책

| 상태 | 옛 정책 (~2024-01) | 현 정책 (2024-02~) |
|---|---|---|
| EIP idle (어디에도 attach 안 됨) | $0.005/hr | $0.005/hr (동일) |
| EIP attached + 인스턴스 running | **무료** | **$0.005/hr ❌** |
| EIP attached + 인스턴스 stopped | $0.005/hr | $0.005/hr |
| EC2 의 일반 public IPv4 (EIP 아님) | **무료** | **$0.005/hr ❌** |

핵심 변화 두 가지:
1. **모든 public IPv4 가 과금 대상** — EIP 든 일반 IP 든 동일하게 시간당 $0.005
2. **EIP attach 가 무료였던 시절 종료** — *어떤 형태로든 IPv4 를 쓰면 비용 발생*

월 환산: `730 hr × $0.005 = ~$3.65/월/IP`

## 의미 — *"안 쓸 이유가 사라짐"*

옛 정책에선 *"EIP 안 쓰고 일반 IP 그대로 두는 게 무료라서 이득"* 이었음. 현 정책에선:

- EIP attached running = $0.005/hr
- 일반 public IP running = $0.005/hr (동일!)

→ **같은 비용이면 stop/start 무관 IP 고정** 이 정답. EIP 안 쓸 이유가 사라짐.

## 우리 선택

새 EC2 에 EIP `43.202.132.38` 할당 + attach. Cloudflare DNS A 레코드 (`@`, `www`) 를 이 EIP 로 갱신. 이후 인스턴스 stop/start 무관 IP 유지 → **DNS 갱신 0 회**.

옛 EC2 가 terminate 되면서 함께 살아남았던 idle EIP (`xxx.63.236`) 도 발견 — release 처리. 옛 EIP 는 *인스턴스 terminate 시 자동 release 안 됨*. 명시 release 안 하면 시간당 $0.005 영원히 청구되는 함정.

## 일반화한 룰

> **EC2 의 public IP 는 EIP 로 고정하라. 인스턴스 terminate 시 EIP 도 같이 release 하라.**

운영 습관:
- EC2 launch 직후 EIP allocate + associate 를 한 세트로
- 인스턴스 종료 절차 마지막에 `aws ec2 release-address` 추가
- 월 1회 `aws ec2 describe-addresses` 로 idle EIP 점검 (청구서 조용한 누수 방지)