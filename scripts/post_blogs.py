"""Backend Live Lab의 핵심 블로그 글을 현재 구현과 동기화한다.

기본 실행은 변경 내용을 출력하는 dry-run이다. 운영 반영은 BLOG_API_TOKEN을
환경 변수로 설정하고 --apply를 명시했을 때만 수행한다.
"""

import argparse
import json
import os
import urllib.error
import urllib.parse
import urllib.request


BASE_URL = os.environ.get("BLOG_API_BASE_URL", "https://minya.life/api/blog").rstrip("/")

posts = [
    {
        "title": "정적 이력서에서 Backend Live Lab으로 확장한 이유",
        "slug": "why-i-built-backend-live-lab",
        "summary": "9년 6개월의 경력을 기술 목록이 아니라 실행 가능한 데모, 제약 조건, 검증 기록으로 설명하기 위해 Live Lab을 구성한 과정.",
        "tags": "포트폴리오,Spring Boot,아키텍처,회고",
        "published": True,
        "thumbnailUrl": None,
        "content": """# 정적 이력서에서 Backend Live Lab으로 확장한 이유

경력 기술서에는 담당 업무와 성과를 기록할 수 있지만, 설계 당시의 제약과 선택지, 실패 이후의 수정 과정까지 전달하기는 어렵다. Backend Live Lab은 이 간극을 보완하기 위해 만든 실행 가능한 포트폴리오다.

## 현재 공개하는 범위

| 영역 | 구현 | 함께 공개하는 한계 |
|---|---|---|
| 경력 Q&A | Spring AI + Gemini, Markdown 경력 원문 주입 | 프롬프트만으로 사실성을 보장할 수 없음 |
| Redis | Postgres 집계 결과 Cache-Aside, TTL, 조회 장애 fallback | stampede·선택적 무효화·고가용성 미구현 |
| Kafka | 3 partitions, blocking retry, DLT 흐름 | 전역 실행 지표와 broker 전달 확인의 한계 |
| DevLog | Markdown 기반 의사결정·장애 기록 | 구현 변경 시 문서도 함께 갱신해야 함 |
| Operations | AWS 배포와 장애 대응 기록 | 제한된 인스턴스 자원과 수동 운영 범위 |

## 설계 기준

- 기능을 추가할 때 현재 구현과 한계를 같은 화면에 표시한다.
- 수치는 측정 환경과 관찰 기간을 함께 기록한다.
- 구현되지 않은 개선안은 로드맵으로 분리한다.
- 코드, 테스트, 화면 설명, 운영 기록이 서로 일치하는지를 완료 조건으로 본다.

Prometheus와 Grafana는 로컬에서 검증했지만 AWS t4g.small에서는 핵심 서비스와 자원을 경쟁해 기본 운영 구성에서 제거했다. Testcontainers 통합 테스트도 아직 도입 전이므로 구현된 기능으로 표시하지 않는다.

이 포트폴리오의 목적은 도구의 개수를 늘리는 것이 아니라, 제한된 환경에서 어떤 기준으로 선택하고 무엇을 보류했는지 검증 가능하게 설명하는 것이다.
""",
    },
    {
        "title": "Spring Boot 패키지 경계를 실용적으로 나눈 기준",
        "slug": "spring-boot-clean-architecture",
        "summary": "클린 아키텍처라는 이름보다 변경 이유와 의존성 경계를 우선한 Spring Boot 패키지 구조와 현재 한계.",
        "tags": "Spring Boot,아키텍처,패키지설계,백엔드",
        "published": True,
        "thumbnailUrl": None,
        "content": """# Spring Boot 패키지 경계를 실용적으로 나눈 기준

이 프로젝트는 `domain`, `application`, `presentation`, `infra`로 패키지를 구분한다. 다만 이를 프레임워크 독립적인 클린 아키텍처를 완성했다고 설명하지 않는다. 일부 domain 객체에는 JPA annotation이 있고 repository도 Spring Data에 의존한다.

```text
com.minyaryung.livelab
├── domain/          도메인 모델과 repository 계약
├── application/     유스케이스와 애플리케이션 서비스
├── presentation/    HTTP controller와 요청·응답 경계
└── infra/           외부 연동, 설정, 공통 구현
```

## 실제로 분리한 경계

S3 파일 저장, OAuth 검증, JWT 발급처럼 외부 시스템이나 공급자 변경 가능성이 있는 기능은 port interface 뒤에 둔다. 반대로 한 가지 구현만 존재하고 교체 요구가 없는 내부 로직에는 인터페이스를 일괄적으로 추가하지 않았다.

판단 기준은 다음 두 가지다.

1. 외부 기술 변경이 application 로직까지 전파되는가?
2. 외부 시스템 없이 핵심 분기를 테스트할 필요가 있는가?

## 인증 구현의 범위

관리 기능은 Google OAuth로 관리자 신원을 확인한 뒤 interceptor에서 JWT를 검사한다. 브라우저 JWT는 HttpOnly·Secure·SameSite=Strict 쿠키로 전달하고, 상태 변경 요청은 별도 CSRF 쿠키와 헤더를 함께 검증한다. 배포용 동기화 스크립트만 환경변수 Bearer 토큰을 사용한다.

현재 구조에서는 다음을 별도로 관리해야 한다.

- 짧은 token 만료와 signing key rotation
- 운영 시작 시 `JWT_SECRET` 필수 검증과 공개 기본값 금지
- HTTPS end-to-end 적용
- 관리자 endpoint 범위 테스트
- 자동화 token을 소스와 로그에 남기지 않는 운영 절차
- 인증 요구가 늘어날 때 Spring Security로 전환할 기준

Markdown renderer는 raw HTML을 escape하고 위험 URL을 제거한다. S3 이미지 업로드는 파일명이나 caller의 Content-Type을 신뢰하지 않고 PNG/JPEG/GIF/WebP signature를 확인해 서버가 MIME과 확장자를 결정한다. 운영 기본 프로필에서는 Actuator `health`, `info`만 노출한다.

관리자 JWT는 브라우저 JavaScript가 읽을 수 없는 HttpOnly 쿠키에 두고 SameSite와 CSRF 이중 검증을 적용한다. 다만 역할과 보호 경로가 늘어나면 커스텀 interceptor보다 Spring Security의 표준 필터 체계로 전환하는 편이 누락 위험을 줄인다.

Google credential 검증은 연결 peer 기준 10분 10회로 제한해 외부 검증 비용과 반복 시도를 제어한다. 관리자 변경 감사 로그에는 성공한 action만 남기고 이메일, JWT, slug, 본문, 원본 파일명은 기록하지 않는다.

관리 API는 제목 300자, 본문 100,000자, slug와 썸네일 URL 형식을 서버에서 검증한다. JSON 문서 256KB·중첩 20단계와 페이지 범위에 상한을 두고, 이미지 업로드는 연결 peer 기준 시간당 20회로 제한한다.

slug는 저장 전 중복을 확인하되 동시 요청 race의 최종 판단은 DB unique constraint에 맡긴다. 중복은 409, 없는 글의 수정·삭제는 404로 구분하며 감사 로그는 저장 성공 이후에만 남긴다.

패키지 이름이나 패턴 적용 여부보다 중요한 것은 변경 영향이 어디까지 전파되는지, 현재 구조가 보장하지 않는 범위를 함께 설명하는 것이다.
""",
    },
    {
        "title": "경력 Q&A의 근거 없는 답변을 줄이는 설계",
        "slug": "ai-chatbot-no-hallucination",
        "summary": "경력 Markdown 전체 주입, 보류 조건, 입력 제한을 적용하고도 남는 LLM 사실성·출처 검증 한계를 정리합니다.",
        "tags": "Spring AI,Gemini,챗봇,LLM,백엔드",
        "published": True,
        "thumbnailUrl": None,
        "content": """# 경력 Q&A의 근거 없는 답변을 줄이는 설계

경력 Q&A에서 가장 큰 위험은 원문에 없는 경험을 모델이 자연스럽게 생성하는 것이다. 이 프로젝트는 경력 Markdown을 system context로 제공하고, 근거가 없으면 보류하도록 지시한다.

## 현재 구조

앱 기동 시 `CareerDataLoader`가 프로필, 기술, 경력, 대표 프로젝트 10건과 한계 문서를 읽는다. 현재 데이터 규모에서는 별도 vector database보다 전체 문맥을 제공하는 방식이 단순하다고 판단했다.

`gaps-and-direction.md`에는 직접 담당하지 않은 영역과 인접 경험을 분리한다. 이를 통해 모델이 “없음”을 답할 데이터 근거를 제공한다.

## 요청 보호

- 질문 길이: 최대 500자
- 호출 제한: 단일 인스턴스 메모리에서 서버가 확인한 peer 주소당 1시간 fixed window 20회, 비활성 bucket 정리
- 모델 설정: temperature 0.2, 최대 출력 1,024 tokens
- 외부 API 재시도: 최대 3회로 제한

## 보장하지 못하는 것

system prompt와 낮은 temperature는 근거 없는 답변 가능성을 줄일 뿐 제거하지 않는다. 현재 응답은 source id를 구조적으로 반환하지 않는다. 임의 `X-Forwarded-For`는 위조 가능하므로 사용하지 않으며, Cloudflare 뒤에서는 여러 사용자가 동일한 peer 주소 한도를 공유할 수 있다. 인메모리 rate limit도 단일 인스턴스라는 전제에 의존한다.

질문 원문과 provider 오류 body는 운영 로그에 남기지 않는다. 요청 길이·처리 시간·HTTP 상태·오류 타입만 기록해 이메일이나 전화번호가 질문에 포함되더라도 로그로 복제되지 않게 한다.

다음 단계는 답변을 `{answer, sources, grounded}` 형태로 받고, 실제 존재하는 source만 허용하는 검증 계층을 추가하는 것이다. 대표 질문과 prompt injection 질문도 회귀 평가 대상으로 만들어야 한다.

LLM 기능은 “환각 방지 완료”가 아니라 현재 제약과 실패 가능성, 보완 계획을 함께 공개할 때 검토 가능한 설계가 된다.
""",
    },
    {
        "title": "Postgres 집계 결과를 Redis Cache-Aside로 다루는 기준",
        "slug": "redis-cache-live-demo",
        "summary": "10만 건 집계 조회에 Redis Cache-Aside를 적용하고, 조회 실패와 무효화 실패를 서로 다른 정책으로 처리한 이유.",
        "tags": "Redis,Spring Cache,Cache-Aside,백엔드,성능",
        "published": True,
        "thumbnailUrl": None,
        "content": """# Postgres 집계 결과를 Redis Cache-Aside로 다루는 기준

Redis 데모는 Postgres의 카테고리별 집계 결과를 캐시하고, 같은 환경에서 DB 직접 조회와 cache hit 응답 시간을 비교한다. Redis는 원본 저장소가 아니며 Postgres를 source of truth로 유지한다.

공개 벤치마크가 DB와 Redis를 반복 호출하므로 연결 peer 기준 실행 누적 400회/분, 전체 캐시 초기화 10회/분의 인메모리 제한을 적용한다. 다중 인스턴스로 확장하면 이 상태는 Redis 같은 공유 저장소로 옮겨야 한다.

## 자료구조와 키

`category-stats::<category>` key에 집계 DTO 목록을 JSON value로 저장한다. 결과 전체를 한 번에 읽고 같은 TTL로 만료하므로 부분 필드 갱신용 Hash보다 String key-value가 현재 접근 패턴에 맞는다.

- cache name: `category-stats`
- key: category
- TTL: 60초
- serializer: `GenericJackson2JsonRedisSerializer`

## 장애 정책 분리

cache GET·PUT 실패는 fail-open으로 처리한다. GET이 실패하면 대상 메서드가 Postgres를 조회하고, PUT이 실패해도 DB 결과는 호출자에게 반환한다.

반면 evict·clear 실패는 예외를 다시 전달한다. 무효화 실패까지 성공처럼 처리하면 stale cache가 남아도 호출자가 알 수 없기 때문이다.

## 현재 구현하지 않은 범위

이 데모에는 상품 쓰기 API가 없다. 화면의 “캐시 비우기”는 비교 실험을 위한 수동 `@CacheEvict(allEntries = true)`이다. DB 저장 이후 선택적 무효화를 구현한 코드가 아니다.

업무 쓰기를 추가한다면 transaction commit 이후 변경된 category key만 삭제하고, rollback과 삭제 실패 시나리오를 통합 테스트해야 한다. stampede 방지를 위한 요청 병합·TTL jitter·bulkhead와 Redis 고가용성 구성도 아직 적용하지 않았다.

Testcontainers 기반 Redis·Postgres 통합 테스트는 로드맵 상태이며 관련 의존성도 실제 테스트를 구현할 때 추가할 예정이다. 현재 자동 테스트는 cache error policy 같은 단위 검증 범위로 한정한다.
""",
    },
    {
        "title": "Kafka 블로킹 재시도와 DLT 데모의 설계 범위",
        "slug": "kafka-dlt-demo",
        "summary": "3개 파티션에서 FixedBackOff 재시도와 DLT 흐름을 구현하고, 처리량과 DLT 지표가 보장하지 못하는 범위를 정리합니다.",
        "tags": "Kafka,DLT,Spring Kafka,재시도,백엔드",
        "published": True,
        "thumbnailUrl": None,
        "content": """# Kafka 블로킹 재시도와 DLT 데모의 설계 범위

이 데모는 주문 이벤트를 3개 partition의 원본 topic으로 발행한다. `orderId`가 17의 배수인 이벤트는 의도적으로 실패시키고, Spring Kafka 오류 처리기를 통해 재시도와 DLT 경로를 관찰한다.

공개 broker 자원 보호를 위해 요청당 최대 2,000건, 연결 peer 기준 10분 누적 5,000건으로 제한하며 초과 요청은 이벤트 발행 전에 429로 거부한다. 제한 상태는 단일 JVM 메모리이므로 다중 인스턴스에서는 공유 제한기가 필요하다.

## 현재 설정

```text
livelab.orders      3 partitions
livelab.orders.DLT  3 partitions
consumer concurrency = 3
FixedBackOff(500ms, 3)
ack mode = record
```

`FixedBackOff(500ms, 3)`은 최초 실패 후 최대 3회 재시도한다. 실패 레코드는 해당 consumer thread를 최대 약 1.5초 점유한다. partition마다 thread를 배치해 다른 partition까지 정지하는 범위를 줄였지만, 같은 partition의 후속 레코드는 영향을 받는다.

DLT도 원본과 같은 3 partitions로 구성했다. `DeadLetterPublishingRecoverer`가 기본적으로 원본 partition을 유지하기 때문이다.

## 화면 지표의 의미

- attempted: 발행을 요청한 수
- acknowledged / publishFailed: producer future 완료 결과
- success: consumer 정상 처리 수
- dlt: recovery handler가 DLT 발행 절차를 수행한 수

현재 dlt 카운터는 별도 consumer가 broker 저장을 확인한 수치가 아니다. 실제 전달 완료를 증명하려면 Testcontainers Kafka에서 DLT 도착, retry 횟수와 header를 검증해야 한다.

각 실행에는 UUID `runId`를 부여하고 event와 producer callback에 함께 전달한다. 현재 서버는 제한된 데모 자원을 보호하기 위해 동시에 한 실행만 허용하며, 중복 publish와 실행 중 reset은 `409 Conflict`로 거부한다. 이전 runId의 늦은 callback은 현재 지표에 반영하지 않는다.

Prometheus와 Grafana는 AWS 기본 구성에서 제거했으므로 consumer lag를 운영 대시보드에서 상시 확인한다고 설명하지 않는다. 현재 페이지의 목적은 제한된 데모에서 재시도 방식과 그 한계를 직접 보여주는 것이다.
""",
    },
]


def request_json(url, method="GET", data=None, token=None):
    headers = {
        "Accept": "application/json",
        "User-Agent": "BackendLiveLabBlogSync/1.0 (+https://github.com/minya8703/live-lab)",
    }
    body = None
    if data is not None:
        body = json.dumps(data, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json; charset=UTF-8"
    if token:
        headers["Authorization"] = f"Bearer {token}"

    request = urllib.request.Request(url, data=body, headers=headers, method=method)
    with urllib.request.urlopen(request, timeout=20) as response:
        payload = response.read()
        return json.loads(payload) if payload else None


def post_exists(slug):
    url = f"{BASE_URL}/{urllib.parse.quote(slug)}"
    try:
        request_json(url)
        return True
    except urllib.error.HTTPError as error:
        if error.code == 404:
            return False
        raise


def sync_post(post, token, apply_changes):
    exists = post_exists(post["slug"])
    action = "UPDATE" if exists else "CREATE"
    print(f"[{('APPLY' if apply_changes else 'DRY-RUN')}] {action} {post['slug']}")
    if not apply_changes:
        return

    if exists:
        url = f"{BASE_URL}/{urllib.parse.quote(post['slug'])}"
        request_json(url, method="PUT", data=post, token=token)
    else:
        request_json(BASE_URL, method="POST", data=post, token=token)


def main():
    parser = argparse.ArgumentParser(description="Live Lab 블로그 글 동기화")
    parser.add_argument("--apply", action="store_true", help="운영 API에 실제 변경을 적용")
    parser.add_argument("--slug", help="지정한 slug 한 건만 처리")
    args = parser.parse_args()

    token = os.environ.get("BLOG_API_TOKEN")
    if args.apply and not token:
        raise SystemExit("--apply 실행에는 BLOG_API_TOKEN 환경 변수가 필요합니다.")

    selected = [post for post in posts if not args.slug or post["slug"] == args.slug]
    if not selected:
        raise SystemExit(f"등록되지 않은 slug입니다: {args.slug}")

    for post in selected:
        try:
            sync_post(post, token, args.apply)
        except urllib.error.HTTPError as error:
            detail = error.read().decode("utf-8", errors="replace")[:300]
            raise SystemExit(f"{post['slug']} 요청 실패: HTTP {error.code} {detail}") from error


if __name__ == "__main__":
    main()
