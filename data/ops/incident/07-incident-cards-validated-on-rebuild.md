---
slug: incident-cards-validated-on-rebuild
title: 회고 카드는 두 번째 셋업에서 자기가 자기를 구한다
date: 2026-06-05
tags: [retrospective, methodology, meta]
---

## 배경

카드 만료 사고 (회고 #05) 로 EC2 를 처음부터 재배포하던 중, **기존 회고 카드 두 장이 새 환경에서 완전히 똑같이 재현**되는 걸 경험. 두 함정 모두 *처음 만났을 때 디버깅에 한 시간씩* 썼던 것인데, 이번엔 회고를 보고 즉시 우회.

이 메타 회고는 *"회고를 작성하는 행위 자체가 미래의 자신을 구하는 시스템"* 이라는 명제의 라이브 증명.

## 재현 1 — SSH 키 권한 함정 (원본: 회고 #01)

**원본 사건**: 컴퓨터명 `MINYA` + 사용자명 `MINYA` 인 환경에서 `icacls` 가 사용자 SID 가 아닌 *머신 SID* 로 키에 권한 부여 → OpenSSH 가 *"Bad permissions"* 로 거부.

**재현 시점**: 새 EC2 띄우고 새 SSH 키 만든 직후. 런북의 `icacls $env:USERPROFILE\.ssh\livelab /grant:r "${env:USERNAME}:R"` 그대로 실행 → 같은 환경 → 같은 머신 SID 함정 → 같은 거부.

**즉시 우회**: 회고에 적힌 *.NET ACL API 로 본인 SID 직접 grant* 패턴 그대로:

```powershell
$path = "$env:USERPROFILE\.ssh\livelab"
$user = [System.Security.Principal.WindowsIdentity]::GetCurrent().User
$acl  = Get-Acl $path
$acl.SetAccessRuleProtection($true, $false)
$acl.Access | ForEach-Object { [void]$acl.RemoveAccessRule($_) }
$rule = New-Object System.Security.AccessControl.FileSystemAccessRule($user, "Read", "Allow")
$acl.SetAccessRule($rule)
Set-Acl -Path $path -AclObject $acl
```

회고 안 봤으면 또 한 시간 디버깅. *시간 절약: 약 50 분*.

## 재현 2 — Docker buildx 0.17 함정 (원본: inception.md 회수 회고 #4)

**원본 사건**: AL2023 의 `dnf install docker` 가 깔아주는 buildx 가 0.17 미만이라 `docker compose build` 가 *"buildx component version 0.17 required"* 로 거부. 첫 디버깅 때 GitHub API 로 latest tag 받으려다 변수가 비어서 404 받는 보조 함정까지.

**재현 시점**: 새 EC2 의 `docker compose --profile prod up -d` 가 build 단계 진입 직후 같은 에러.

**즉시 우회**: 회고에 적힌 *명시 고정 버전* 패턴 그대로:

```bash
BUILDX_VERSION=v0.17.1   # 'latest' grep 파싱은 빈 변수 함정이 있어서 명시 고정
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -fsSL "https://github.com/docker/buildx/releases/download/${BUILDX_VERSION}/buildx-${BUILDX_VERSION}.linux-arm64" \
  -o /usr/local/lib/docker/cli-plugins/docker-buildx
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-buildx
```

회고 안 봤으면 또 30 분 디버깅 + 404 함정으로 추가 시간 낭비. *시간 절약: 약 40 분*.

추가로, `infra/aws/user-data.sh` 의 4.5 단계에 같은 명령을 영구 추가 — 다음 EC2 부터는 user-data 자동 처리로 회고도 안 봐도 됨. *3 차 셋업부터는 함정 자체가 사라짐*.

## 데이터 — "회고 ROI"

| 함정 | 원본 디버깅 시간 | 재현 시 회고 활용 시간 | 절약 |
|---|---:|---:|---:|
| SSH SID 권한 | ~60 분 | ~10 분 (.NET 명령 copy + 실행) | **50 분** |
| Buildx 0.17 | ~70 분 (404 함정 포함) | ~5 분 (curl 3 줄) | **65 분** |
| 합계 | 130 분 | 15 분 | **115 분** |

회고 작성에 들었던 시간: 약 30 분 (두 장 합계). **115 분 ÷ 30 분 = ROI 약 3.8 배**. 이건 *두 번째* 셋업 한 번에서만의 수치 — 미래의 *N 번째* 셋업까지 합치면 ROI 가 선형으로 커짐.

## 일반화한 룰

> **회고 카드는 콘텐츠가 아니라 시간이 절약되는 시스템이다.**

세 가지 운영 원칙:

1. **회고 한 장의 형식 = 증상 → 가설 → 결정적 단서 → 진짜 원인 → 해결 → 일반화 → (코드 또는 명령) → 면접 답변용**
   - 6 개월 후의 자신이 *5 분 안에 같은 함정을 우회* 할 수 있도록
2. **"함정 자체를 없애는" 회고가 가장 강력**
   - 회고 → 런북/스크립트로 영구 박는 패턴 (예: user-data.sh 의 buildx 단계)
   - 회고 → 회고만 남기는 패턴보다 두 단계 위
3. **재현된 사건은 따로 메타 회고로 남긴다**
   - 회고의 가치 자체가 *수치로 증명* 됨
   - 면접에서 *"실패 자체를 콘텐츠로 만든 가장 좋은 사례"* 로 인용 가능