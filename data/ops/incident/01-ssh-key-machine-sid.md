---
slug: ssh-key-machine-sid
title: SSH 키가 거부될 때 — Windows 의 동음 머신명·사용자명 함정
date: 2026-06-04
tags: [aws, ssh, windows, debugging]
---

## 증상

EC2 에 SSH 접속 시도 → `Permission denied (publickey)` 또는 `bad permissions` 경고. icacls 결과가 다음처럼 이상함:

```
C:\Users\MINYA\.ssh\livelab MINYA\:(R)
                            ↑ 사용자명이 비어있음
```

## 의심한 가설 (시간순)

1. passphrase 잘못 입력 — 키 재생성으로 부정
2. AWS Key Pair 와 로컬 fingerprint mismatch — 확인 결과 일치
3. 권한이 너무 열려있음 → 좁히면 너무 좁아짐 → 두 함정 사이를 진동
4. 키 파일 위치 (`.ssh` 가 OneDrive 동기화 폴더라서) — `C:\sshkeys` 로 이전 시도

## 결정적 단서

icacls 출력의 SID `S-1-5-21-1703555909-2164541681-4113425133` 분해:
- `S-1-5-21-X-Y-Z` 형식 = **머신/도메인 SID** (3 segments after `21`)
- 사용자 SID 는 `S-1-5-21-X-Y-Z-RID` (4 segments, 끝에 `-1001` 같은 RID 추가)

→ 키 권한이 **사용자가 아니라 머신 자체**에 부여된 상태였음.

## 진짜 원인

| 이름 | 값 |
|---|---|
| 컴퓨터 이름 | `MINYA` |
| 사용자 이름 | `minya` |

Windows 의 이름 해석이 case-insensitive 라 `minya` 를 컴퓨터(`MINYA`) 로 해석. icacls `/grant:r minya:F` 가 머신 SID 에 권한 부여, 사용자에겐 권한 0.

## 해결 — SID 직접 grant 로 이름 해석 우회

```powershell
$key = "C:\sshkeys\livelab"
$userSid = [System.Security.Principal.WindowsIdentity]::GetCurrent().User.Value
icacls $key /reset
icacls $key /inheritance:r
icacls $key /grant:r "*${userSid}:F"
```

`*` 접두사가 *"이건 SID 다, 이름 해석 하지 마라"* 의미.

## 일반화한 룰

1. **이름 충돌 가능한 환경에서 ACL 조작 시 SID 를 진실의 근원으로**
2. Windows 의 *"이름→SID"* 변환은 시스템 컨텍스트에 따라 다르게 매핑
3. `icacls` 출력의 사용자명 칸이 비어있거나 이상하면 SID 부터 확인
4. PowerShell 의 `.NET ACL API` (`Get-Acl`/`Set-Acl`) 도 같은 함정에 걸림 — `SetSecurityPrivilege` 에러로 위장