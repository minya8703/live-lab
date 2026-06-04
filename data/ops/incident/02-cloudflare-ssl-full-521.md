---
slug: cloudflare-ssl-full-521
title: Cloudflare 521 — SG·DNS·origin 다 정상인데 사이트가 안 보일 때
date: 2026-06-04
tags: [cloudflare, ssl, debugging]
---

## 증상

```
$ curl -I https://minya.life
HTTP/1.1 521 <none>
Server: cloudflare
CF-RAY: a065a490883da6a8-HKG
```

521 = *"Web server is down"* (Cloudflare → origin 연결 거부).

## 의심한 가설 (시간순)

| 가설 | 검증 | 결과 |
|---|---|---|
| Cloudflare DNS A 레코드 IP 가 옛 것 | `nslookup` + 콘솔 비교 | 일치 ✓ |
| EC2 보안 그룹이 Cloudflare IP 차단 | SG inbound 확인 | `0.0.0.0/0` 으로 열림 ✓ |
| EC2 origin 자체가 죽어있음 | PC → EC2 IP 직접 호출 | `200 OK` ✓ |
| EC2 안 docker compose 다운 | `docker compose ps` | `Up (healthy)` ✓ |
| **Cloudflare SSL/TLS 모드** | SSL/TLS → Overview 페이지 | **"Current encryption mode: Full"** ← 범인 |

## 결정적 단서

Cloudflare Overview 페이지의 다이어그램:

```
Browser → [🔒] → Cloudflare → [🔒] → Origin Server
                                  ↑
                            Full = 여기도 HTTPS 시도
                                  → EC2 는 HTTP 80 만 → 거부 → 521
```

## 진짜 원인

Cloudflare SSL/TLS 모드 4가지:

| 모드 | 사용자 ↔ Cloudflare | Cloudflare ↔ Origin |
|---|---|---|
| Off | HTTP | HTTP |
| **Flexible** | HTTPS | HTTP ← 우리 케이스 |
| Full | HTTPS | HTTPS (자체 서명 OK) |
| Full (strict) | HTTPS | HTTPS (신뢰 가능 CA 인증서 필수) |

기본 설정이 보통 **Full** 인데, 우리 EC2 는 80 만 노출하고 443 인증서가 없어서 521.

## 해결

SSL/TLS → Overview → Configure → **Flexible** 선택. 30초~1분 안에 반영.

## 왜 521 진단이 까다로운가

- 메시지가 *"Web server is down"* 이라 origin 죽었다고 오해
- 실제는 origin 살아있고 *"443 으로 가서 거부됨"* 인데 그렇게 안 보임
- 보안 그룹·DNS·docker 까지 다 점검해도 같은 521 만 반복

## 일반화한 룰 — Cloudflare 521 진단 순서

1. **PC → Origin IP 직접 호출** (Cloudflare 우회) → 200 이면 origin OK
2. **Cloudflare DNS A 레코드 IP** 와 Origin IP 비교 → 일치하면 SSL 모드 의심
3. **SSL/TLS → Overview** 페이지의 *Current encryption mode* 확인
4. EC2 에 자체 인증서 없으면 → **Flexible** 가 유일한 정답

## 면접 답변용

> *"Cloudflare 521 은 보통 SSL/TLS 모드가 원인입니다. Full 또는 Full(strict) 면 Cloudflare 가 origin 의 443 으로 HTTPS 시도를 하는데, origin 에 인증서 없는 무료 호스팅 환경이라면 거부됩니다. Flexible 로 바꾸면 origin 은 HTTP 80 만 받으면 되니까 EC2 손 안 대고 끝나죠. 보안 그룹·DNS 부터 의심하기 쉬운데, 우선 SSL 모드부터 보는 게 시간 절약입니다."*
