---
slug: cloudflare-ssl-full-521
title: Cloudflare 521 — origin 포트와 SSL 모드 불일치 진단
date: 2026-06-04
tags: [cloudflare, ssl, debugging]
---

## 증상

```text
$ curl -I https://minya.life
HTTP/1.1 521
Server: cloudflare
```

Cloudflare가 origin에 연결하지 못해 521을 반환했다.

## 가설과 검증

| 가설 | 검증 | 결과 |
|---|---|---|
| DNS A 레코드가 이전 IP를 가리킴 | DNS와 EC2 주소 비교 | 일치 |
| 보안 그룹이 프록시 연결을 차단 | 인바운드 규칙 확인 | 당시에는 전체 공개 상태라 원인에서 제외 |
| origin 또는 컨테이너가 중지됨 | origin 직접 호출, 컨테이너 상태 확인 | 정상 |
| SSL 모드와 origin 포트가 불일치 | Cloudflare SSL 모드와 origin 리스닝 포트 비교 | `Full`은 HTTPS를 요구하지만 origin은 HTTP만 제공 |

## 원인

Cloudflare의 `Full` 모드는 origin에도 HTTPS로 연결한다. 당시 origin은 80 포트의 HTTP만 제공했기 때문에 연결이 거부됐다. 오류 메시지만 보면 서버 장애처럼 보이지만 실제 원인은 전송 구간의 프로토콜 불일치였다.

## 당시 복구와 장기 조치

- 당시 복구: 서비스 가용성을 먼저 회복하기 위해 `Flexible`로 임시 전환
- 장기 조치: origin 인증서를 구성하고 `Full (strict)`로 전환
- 접근 제어: origin은 Cloudflare 프록시에서 오는 요청만 허용

`Flexible`은 이 장애를 우회할 수 있지만 일반적인 해결책이나 최종 보안 상태는 아니다.

## 일반화한 진단 순서

1. origin 프로세스와 리스닝 포트를 확인한다.
2. Cloudflare DNS가 현재 origin을 가리키는지 확인한다.
3. SSL 모드가 요구하는 origin 프로토콜과 인증서 상태를 대조한다.
4. 보안 그룹과 방화벽이 필요한 프록시 경로만 허용하는지 확인한다.
5. 복구 후 `Full (strict)` 연결과 origin 직접 접근 차단 여부를 별도로 검증한다.
