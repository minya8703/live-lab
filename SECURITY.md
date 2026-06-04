# 🔒 보안 가이드

## 개요

이 프로젝트는 **민감한 자격증명(credentials)이 git에 절대 올라가지 않도록** 다층 방어 시스템을 구성하고 있습니다.

---

## 🛡️ 다층 방어 구조

### 1️⃣ `.gitignore` — 파일 수준 차단

`.gitignore`에 등록된 파일은 자동으로 git 추적에서 제외됩니다:

```
# 환경 설정
.env
.env.*
!.env.example
application-local.properties
application-local.yml

# 암호화 키
*.key
*.pem
*.pfx
*.jks
*.p12
*.keystore

# 자격증명 파일
credentials/
secrets/

# 데이터베이스 백업
*.sql
*.dump
*.bak
```

**중요**: 새로 추가된 민감한 파일 확장자는 반드시 `.gitignore`에 등록하세요.

### 2️⃣ **Git Pre-commit Hook** — 커밋 시점 검사

git commit을 실행할 때 자동으로 민감한 패턴을 스캔합니다:

- ✅ AWS Access Key (`AKIA...`)
- ✅ API Key / 비밀번호 / Token 선언 (`password=`, `api_key=`)
- ✅ Private Key 헤더 (`BEGIN RSA PRIVATE KEY` 등)
- ✅ 민감한 파일 확장자 (`.env`, `.key`, `.pem`, `.jks` 등)

**실행 방식**:
- **Linux/Mac**: Bash 스크립트 (`.git/hooks/pre-commit`)
- **Windows**: PowerShell 스크립트 (`.git/hooks/pre-commit.ps1`) + Git Bash 연동

---

## 📋 올바른 자격증명 관리

### ✅ 권장 방법 — 환경 변수 사용

```properties
# ❌ 절대 하지 마세요 — 이렇게 하면 hook이 차단합니다
spring.ai.openai.api-key=sk-12345abcdef

# ✅ 대신 이렇게 하세요 — 환경 변수
spring.ai.openai.api-key=${GOOGLE_API_KEY:}
```

### 📝 로컬 설정 (`.env`)

**`.env.example` 복사 → `.env` 생성 → 값 입력**:

```bash
cp .env.example .env
```

`.env` 파일은:
- 자동으로 `.gitignore` 적용 (커밋 불가)
- Spring Boot (`spring-dotenv`) 자동 로드
- Docker Compose도 자동 로드

### 예시 설정

```env
# .env
GOOGLE_API_KEY=AIza-YOUR-ACTUAL-KEY-HERE
POSTGRES_PASSWORD=your-secure-password-123
AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY
```

---

## 🚨 실수로 민감한 데이터를 커밋하려 할 때

### 상황: Hook이 커밋을 차단함

```bash
$ git commit -m "Add config"
🔍 Scanning for security issues...
❌ Found suspicious pattern: password\s*[:=]
⛔ Commit blocked: Security-sensitive content detected!
```

### 💡 해결 방법

1. **원인 파악**: 어느 파일에 민감한 데이터가 있는지 확인
   ```bash
   git diff --cached | grep -i "password"
   ```

2. **파일 수정**: 민감한 부분을 제거하거나 환경변수로 변경

3. **`.gitignore` 추가**: 필요하면 파일 타입을 `.gitignore`에 추가
   ```bash
   echo "*.secret" >> .gitignore
   ```

4. **Staging 다시**: 수정 후 다시 stage
   ```bash
   git add .
   git commit -m "Add config"
   ```

### ⚠️ 정말 필요한 경우만 우회 (비권장)

```bash
git commit --no-verify  # Hook 무시하고 커밋
```

**⚠️ 주의**: 이 옵션은 긴급 상황에만 사용하고, 이후 반드시 리뷰 과정을 거쳐야 합니다.

---

## 📦 이미 커밋된 민감한 데이터?

**Git History에서 완전히 제거해야 합니다:**

```bash
# 1. 전체 히스토리에서 민감한 파일 삭제
git filter-branch --tree-filter 'rm -f .env' HEAD

# 2. 강제 푸시 (협업 시 팀 공지 필수!)
git push origin main --force
```

또는 더 안전하게:
```bash
# BFG Repo-Cleaner 사용 (권장)
bfg --delete-files .env
git push origin main --force
```

---

## 🔐 추가 보안 체크리스트

- [ ] 모든 `.env` 파일이 `.gitignore`에 등록되어 있나?
- [ ] Private Key (`.key`, `.pem`)가 어디에도 저장되지 않았나?
- [ ] API Key / 비밀번호가 소스 코드에 하드코딩되지 않았나?
- [ ] Docker 환경 변수도 `.env` 기반으로 설정되었나?
- [ ] Pre-commit hook이 정상 작동하나? (`git commit` 시 검사 메시지 확인)

---

## 🤝 팀 협업 시

1. **`.env.example` 항상 최신화**: 새 설정 항목 추가 시 반드시 example 파일도 업데이트
   ```bash
   git add .env.example  # ✅ OK
   git add .env          # ❌ 절대 금지
   ```

2. **온보딩 가이드**: 신규 팀원에게 `.env.example` → `.env` 설정 안내

3. **리뷰 시**: PR에서 민감한 내용이 포함되지 않았는지 확인

---

## 📚 참고 자료

- **Spring Boot 환경변수**: https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config
- **Git Hooks 공식**: https://git-scm.com/book/en/v2/Customizing-Git-Git-Hooks
- **OWASP 자격증명 관리**: https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html

---

**마지막으로**: 이 설정만으로는 완벽하지 않습니다. 항상 **코드 리뷰**와 **개발자 교육**을 병행하세요! 🛡️
