---
slug: cloudflare-ssl-mode-decision
title: Cloudflare SSL 모드 재검토 — 장애 복구와 안전한 운영 기준 분리
date: 2026-06-04
tags: [cloudflare, ssl, architecture, security]
---

## 재검토 배경

초기 배포에서는 origin(EC2)에 인증서가 없어 Cloudflare의 `Full` 연결이 실패했고, 서비스 복구를 위해 `Flexible`로 전환했다. 당시에는 정적 포트폴리오 중심이라는 이유로 이를 최종 구조처럼 판단했다.

이후 OAuth 로그인, JWT 인증, 관리자 기능과 챗봇 요청이 추가되면서 전제가 달라졌다. 사용자와 Cloudflare 사이는 HTTPS여도, `Flexible`에서는 Cloudflare와 origin 사이가 HTTP다. 전송 구간을 둘로 나누어 위험을 평가해야 한다.

## 모드별 판단

| 모드 | 사용자 ↔ Cloudflare | Cloudflare ↔ Origin | 운영 판단 |
|---|---|---|---|
| Off | HTTP | HTTP | 사용하지 않음 |
| Flexible | HTTPS | HTTP | 장애 복구를 위한 임시 선택. 인증 기능이 있는 운영 환경의 목표 상태로는 부적합 |
| Full | HTTPS | HTTPS, 인증서 검증 없음 | 암호화는 되지만 origin 신원 검증이 부족함 |
| **Full (strict)** | HTTPS | HTTPS, 유효한 인증서 검증 | **운영 목표 상태** |

## 수정된 의사결정

`Flexible`은 인증서가 없던 초기 단계의 복구 수단이었으며, 장기 운영 선택으로 정당화하지 않는다. 운영 목표는 다음과 같다.

1. origin에 Cloudflare Origin CA 또는 신뢰 가능한 CA 인증서를 설치한다.
2. Cloudflare SSL/TLS 모드를 `Full (strict)`로 전환한다.
3. origin 접근은 Cloudflare 프록시 경로로 제한하고 직접 접근을 차단한다.
4. 인증서 만료와 HTTPS 연결 실패를 배포 체크리스트 및 모니터링 대상에 포함한다.

실제 전환을 검증하기 전까지는 이를 **완료 상태가 아닌 보안 부채**로 기록한다.

## 배운 점

아키텍처 의사결정은 최초 전제가 유지되는지 다시 검토해야 한다. 정적 페이지였던 서비스에 인증과 관리 기능이 추가되면, 같은 SSL 설정도 더 이상 같은 위험 수준이 아니다. 장애를 빠르게 복구한 선택과 안전한 운영의 최종 상태를 구분해 기록한다.
