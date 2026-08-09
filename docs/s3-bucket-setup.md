# S3 버킷 생성 매뉴얼 (블로그 이미지 저장용)

Live Lab 블로그의 이미지/파일 업로드를 위한 AWS S3 버킷 생성 가이드.

---

## 1. 사전 준비

- AWS 계정 로그인 상태
- 리전: **미국 동부(버지니아 북부) us-east-1**

---

## 2. S3 버킷 생성

### 2-1. 버킷 만들기 진입

1. AWS 콘솔 > **S3** 검색 > S3 대시보드 진입
2. **[버킷 만들기]** 클릭

### 2-2. 일반 구성

| 항목 | 설정값 |
|------|--------|
| AWS 리전 | **미국 동부(버지니아 북부) us-east-1** |
| 버킷 유형 | **범용** (기본값) |
| 버킷 네임스페이스 | **글로벌 네임스페이스** (기본값) 또는 **계정 리전 네임스페이스(권장)** |
| 버킷 이름 | `livelab-blog` |

> **버킷 네임스페이스 선택 기준**
> - **글로벌 네임스페이스**: 기존 방식. 버킷 이름이 전 세계 고유해야 함. 이름이 겹치면 `livelab-blog-minya` 등으로 변경 필요.
> - **계정 리전 네임스페이스(권장)**: 2025년 추가된 옵션. 같은 이름이 다른 계정에 있어도 충돌 안 함. 단, 공개 URL 형식이 다를 수 있으므로 6번 단계에서 URL 확인 필요.

### 2-3. 객체 소유권

- **ACL 비활성화됨(권장)** 선택
- 객체 소유권: **버킷 소유자 적용** (자동 선택됨)

### 2-4. 퍼블릭 액세스 차단 설정

블로그 이미지는 누구나 볼 수 있어야 하므로 퍼블릭 읽기가 필요합니다.

1. **"모든 퍼블릭 액세스 차단" 체크 해제**
2. 아래 4개 항목이 모두 **해제**된 것을 확인:
   - [ ] 새 ACL을 통해 부여된 버킷 및 객체에 대한 퍼블릭 액세스 차단
   - [ ] 임의의 ACL을 통해 부여된 버킷 및 객체에 대한 퍼블릭 액세스 차단
   - [ ] 새 퍼블릭 버킷 또는 액세스 지점 정책을 통해 부여된 퍼블릭 액세스 차단
   - [ ] 임의의 퍼블릭 버킷 또는 액세스 지점 정책을 통해 부여된 퍼블릭 액세스 차단
3. 경고 확인란 체크: **"현재 설정으로 인해 이 버킷과 버킷 안의 객체가 퍼블릭 상태가 될 수 있음을 알고 있습니다."**

### 2-5. 버킷 버전 관리

- **비활성화** 선택 (이미지 저장이므로 버전 관리 불필요, 비용 절감)

### 2-6. 태그 (선택 사항)

- 스킵 가능. 관리 목적으로 추가하려면:
  - 키: `Project` / 값: `livelab`

### 2-7. 기본 암호화

| 항목 | 설정값 |
|------|--------|
| 암호화 유형 | **Amazon S3 관리형 키(SSE-S3)를 사용한 서버 측 암호화** |
| 버킷 키 | **활성화** |

> DSSE-KMS, SSE-KMS는 추가 요금이 발생합니다. 포트폴리오 용도에서는 SSE-S3으로 충분합니다.

### 2-8. 고급 설정

- 기본값 유지 (Object Lock 비활성화)

### 2-9. 생성

- **[버킷 만들기]** 클릭

---

## 3. 버킷 정책 설정 (퍼블릭 읽기)

버킷 생성 후 업로드된 이미지를 누구나 읽을 수 있도록 정책을 추가합니다.

1. 생성된 버킷 `livelab-blog` 클릭
2. **[권한]** 탭 선택
3. **버킷 정책** 섹션 > **[편집]** 클릭
4. 아래 JSON 붙여넣기:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "PublicReadGetObject",
            "Effect": "Allow",
            "Principal": "*",
            "Action": "s3:GetObject",
            "Resource": "arn:aws:s3:::livelab-blog/*"
        }
    ]
}
```

5. **[변경 사항 저장]** 클릭

> **주의**: `livelab-blog` 부분을 실제 버킷 이름으로 변경하세요.
> 계정 리전 네임스페이스를 사용한 경우 ARN 형식이 다를 수 있습니다. 버킷 속성 탭에서 실제 ARN을 확인하세요.

---

## 4. CORS 설정

브라우저에서 이미지를 로드할 때 CORS 오류를 방지합니다.

1. **[권한]** 탭 > **CORS(Cross-Origin Resource Sharing)** 섹션 > **[편집]**
2. 아래 JSON 붙여넣기:

```json
[
    {
        "AllowedHeaders": ["*"],
        "AllowedMethods": ["GET"],
        "AllowedOrigins": ["*"],
        "ExposeHeaders": [],
        "MaxAgeSeconds": 3600
    }
]
```

3. **[변경 사항 저장]** 클릭

---

## 5. IAM 사용자 생성 (액세스 키 발급)

애플리케이션이 S3에 업로드하기 위한 전용 사용자를 생성합니다.
루트 계정의 키를 직접 사용하지 않고, 최소 권한의 전용 사용자를 만드는 것이 AWS 보안 모범 사례입니다.

### 5-1. IAM 사용자 생성

1. AWS 콘솔 > **IAM** 검색 > 왼쪽 메뉴 **[사용자]**
2. **[사용자 생성]** 클릭
3. 사용자 이름: `livelab-s3-uploader`
4. "AWS Management Console에 대한 사용자 액세스 권한 제공" 체크 **해제** (프로그래밍 전용)
5. **[다음]** 클릭

### 5-2. 권한 설정

1. **직접 정책 연결** 선택
2. **[정책 생성]** 클릭 (새 탭에서 열림)
3. **JSON** 탭 선택 후 아래 입력:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "LiveLabBlogS3Access",
            "Effect": "Allow",
            "Action": [
                "s3:PutObject",
                "s3:DeleteObject"
            ],
            "Resource": "arn:aws:s3:::livelab-blog/*"
        }
    ]
}
```

4. **[다음]** 클릭
5. 정책 이름: `LiveLabBlogS3Policy`
6. **[정책 생성]** 클릭
7. 사용자 생성 탭으로 돌아가서 새로고침 > `LiveLabBlogS3Policy` 검색 후 체크
8. **[다음]** > **[사용자 생성]**

### 5-3. 액세스 키 발급

1. 생성된 `livelab-s3-uploader` 사용자 클릭
2. **[보안 자격 증명]** 탭
3. 액세스 키 섹션 > **[액세스 키 만들기]** 클릭
4. 사용 사례: **AWS 외부에서 실행되는 애플리케이션** 선택
5. **[다음]** > **[액세스 키 만들기]**
6. **Access Key ID**와 **Secret Access Key**를 복사하여 안전하게 저장

> 이 페이지를 벗어나면 Secret Access Key를 다시 볼 수 없습니다. 반드시 즉시 저장하세요.

---

## 6. 애플리케이션 설정

### 6-1. 공개 URL 확인

버킷의 공개 URL은 네임스페이스 유형에 따라 다릅니다.

| 네임스페이스 | URL 형식 |
|-------------|---------|
| 글로벌 | `https://livelab-blog.s3.us-east-1.amazonaws.com` |
| 계정 리전 | 버킷 > **[속성]** 탭에서 확인 |

버킷 > **[속성]** 탭 최상단의 **Amazon 리소스 이름(ARN)** 아래에 표시되는 URL을 확인하세요.

### 6-2. EC2 서버 `.env` 파일에 추가

```bash
# S3 Storage (블로그 이미지)
STORAGE_ENDPOINT=https://s3.us-east-1.amazonaws.com
STORAGE_REGION=us-east-1
STORAGE_ACCESS_KEY=AKIA...여기에_Access_Key_ID
STORAGE_SECRET_KEY=여기에_Secret_Access_Key
STORAGE_BUCKET=livelab-blog
STORAGE_PUBLIC_URL=https://livelab-blog.s3.us-east-1.amazonaws.com
```

### 6-3. 설정값 정리

| .env 키 | 값 | 설명 |
|----------|-----|------|
| `STORAGE_ENDPOINT` | `https://s3.us-east-1.amazonaws.com` | S3 API 엔드포인트 |
| `STORAGE_REGION` | `us-east-1` | 버킷 리전 |
| `STORAGE_ACCESS_KEY` | `AKIA...` | 5-3에서 발급한 Access Key ID |
| `STORAGE_SECRET_KEY` | (비밀) | 5-3에서 발급한 Secret Access Key |
| `STORAGE_BUCKET` | `livelab-blog` | 2-2에서 설정한 버킷 이름 |
| `STORAGE_PUBLIC_URL` | `https://livelab-blog.s3.us-east-1.amazonaws.com` | 6-1에서 확인한 공개 URL |

### 6-4. 업로드된 파일의 공개 URL 형식

```
https://livelab-blog.s3.us-east-1.amazonaws.com/blog/2026/07/a1b2c3d4-image.png
```

앱 코드에서 `blog/YYYY/MM/UUID-파일명` 경로를 자동 생성합니다.

---

## 7. 검증

### 7-1. 업로드 테스트 (앱 실행 후)

```bash
# 브라우저 쿠키가 아닌, 환경변수로 관리하는 단기 자동화 JWT 사용
curl -X POST https://your-domain/api/blog/upload \
  -H "Authorization: Bearer YOUR_AUTOMATION_JWT" \
  -F "file=@test-image.png"
```

성공 응답:
```json
{
  "url": "https://livelab-blog.s3.us-east-1.amazonaws.com/blog/2026/07/a1b2c3d4-test-image.png"
}
```

### 7-2. 브라우저에서 확인

응답의 URL을 브라우저에 붙여넣어 이미지가 표시되면 정상입니다.

### 7-3. AWS CLI로 확인 (선택)

```bash
aws s3 ls s3://livelab-blog/blog/ --recursive --region us-east-1
```

---

## 8. 비용 참고

| 항목 | 프리 티어 (12개월) | 이후 |
|------|-------------------|------|
| 저장소 | 5GB 무료 | $0.023/GB/월 (us-east-1) |
| PUT 요청 | 2,000건 무료 | $0.005/1,000건 |
| GET 요청 | 20,000건 무료 | $0.0004/1,000건 |
| 데이터 전송 (인터넷) | 100GB 무료 | $0.09/GB |

포트폴리오 블로그 규모(월 수십 장 이미지)에서는 **프리 티어로 충분**하며, 이후에도 월 $0.1 미만입니다.

> us-east-1은 AWS에서 가장 저렴한 리전입니다. EC2가 다른 리전에 있으면 교차 리전 데이터 전송 비용($0.02/GB)이 소량 발생하지만, 이미지 업로드 트래픽이 적으므로 무시할 수준입니다.

---

## 트러블슈팅

| 증상 | 원인 | 해결 |
|------|------|------|
| 앱 로그에 `S3 access key not configured` | `.env`에 `STORAGE_ACCESS_KEY` 미설정 또는 빈 값 | `.env` 확인 후 앱 재시작 |
| 업로드 시 `403 Access Denied` | IAM 정책에 `s3:PutObject` 누락 | 5-2 IAM 정책 확인 |
| 이미지 URL 접근 시 `403 Forbidden` | 버킷 정책 미적용 또는 퍼블릭 액세스 차단 ON | 2-4 퍼블릭 액세스 해제 + 3번 버킷 정책 재확인 |
| 브라우저 콘솔 CORS 오류 | CORS 설정 누락 | 4번 단계 적용 |
| `The bucket does not allow ACLs` | 객체 소유권이 ACL 활성화로 설정됨 | 2-3에서 ACL 비활성화 확인 |
| `InvalidBucketName` | 버킷 이름에 대문자나 특수문자 포함 | 소문자, 숫자, 하이픈만 사용 (3~63자) |
