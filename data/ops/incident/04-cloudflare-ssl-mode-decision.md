---
slug: cloudflare-ssl-mode-decision
title: Cloudflare SSL 모드 의사결정 — Flexible vs Full vs Full(strict)
date: 2026-06-04
tags: [cloudflare, ssl, architecture]
---

## 결정 배경

이 사이트는 EC2 에 nginx + Let's Encrypt 같은 SSL 스택을 안 깔았습니다. 의도적으로요. Cloudflare 가 무료로 HTTPS 처리하니까 origin 은 HTTP 만 받아도 충분.

그런데 Cloudflare 의 SSL/TLS 모드를 4개 중 어느 걸로 하느냐가 운영 부담을 결정합니다.

## 4가지 모드 비교

| 모드 | 사용자 ↔ Cloudflare | Cloudflare ↔ Origin | EC2 에 인증서 필요? | 운영 부담 |
|---|---|---|---|---|
| Off | HTTP | HTTP | ❌ | 최소 — 그러나 HTTPS 자체 없음 |
| **Flexible** | HTTPS ✅ | HTTP | ❌ | 최소 (✅ 우리 선택) |
| Full | HTTPS ✅ | HTTPS (자체 서명 OK) | ✅ (자체 서명 가능) | 중간 — 인증서 갱신 필요 |
| Full (strict) | HTTPS ✅ | HTTPS (신뢰 CA 만) | ✅ (Let's Encrypt 등) | 높음 — 90일 갱신 + 모니터링 |

## 우리 선택: Flexible

이유:

1. **EC2 손 0** — 인증서 관리 안 함
2. **Cloudflare ↔ EC2 사이가 AWS 백본 또는 인터넷** — Cloudflare 가 Sydney PoP 에서 우리 ap-southeast-2 EC2 로 가는 경로는 거의 같은 지역. 평문이라도 외부 노출 시간이 극히 짧음
3. **무료** — Let's Encrypt 자동화는 무료지만, 인증서 갱신 실패 시 사이트 다운 가능성
4. **Flexible 이 521 안 일으킴** — Full 모드는 origin 인증서 없으면 521

## 왜 Full 로 안 가는가

이 사이트 트래픽은:
- 챗봇 질문/응답 (저민감)
- 정적 페이지 (공개 정보)
- Live 데모 메트릭 (공개)

**API 키·비밀번호·개인정보를 origin 으로 보내는 흐름 없음.** Cloudflare → Origin 간 평문이어도 노출되는 데이터가 본인 공개 포트폴리오뿐.

운영 환경에서 사용자 인증·결제·민감 데이터가 흐르는 경우엔 당연히 Full (strict) 가 정답.

## 언제 Full 또는 Full(strict) 로 올릴까

- EC2 안에 사용자 로그인·세션 처리 추가 시
- 결제 흐름 추가 시
- 회사 컴플라이언스 요구 (PCI-DSS, SOC2 등)

업그레이드 절차:
1. EC2 에 `certbot` 설치 → Let's Encrypt 발급
2. nginx 또는 docker-compose 로 443 노출
3. Cloudflare → Full → Full(strict) 점진 전환
4. 인증서 자동 갱신 cron 모니터링

## 의사결정 한 줄

> *"오버킬을 피하고 trade-off 를 명시한다."*

이 사이트는 *"AWS·운영 학습 콘텐츠"* 이지 *"민감 데이터 처리 SaaS"* 가 아님. 그 컨텍스트에서 Flexible 이 정답.

## 면접 답변용

> *"Cloudflare SSL 모드는 Flexible 로 했습니다. EC2 에 자체 인증서를 안 까는 게 운영 부담을 줄이는 선택이에요. 다만 Cloudflare ↔ Origin 사이가 평문이라는 trade-off 가 있어서, 사용자 인증이나 민감 데이터가 흐르면 그땐 Full(strict) 로 올리고 Let's Encrypt 자동 갱신을 붙입니다. 지금 사이트는 공개 포트폴리오라서 그 trade-off 가 정당화됐다고 봤어요."*
