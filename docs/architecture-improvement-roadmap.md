# Live Lab 아키텍처 개선 로드맵

## 1. 목적

이 문서는 Kafka, Redis, AI 경력 Q&A의 현재 구현을 포트폴리오 관점에서 검증하고,
한 번에 한 가지 위험을 수정하면서 코드, 테스트, 화면 설명이 함께 개선되도록 관리한다.

완료의 기준은 "코드를 추가했다"가 아니라 다음 네 가지가 일치하는 상태다.

1. 구현이 의도한 실패 시나리오에서도 동작한다.
2. 자동화 테스트가 그 동작을 증명한다.
3. 화면에 표시하는 수치의 의미가 실제 측정값과 일치한다.
4. README와 Live Lab 설명이 현재 코드와 일치한다.

## 2. 현재 평가 요약

| 영역 | 강점 | 핵심 위험 |
|---|---|---|
| Kafka | 3 partitions, concurrency=3, record ack, retry/DLT와 trade-off 공개 | 전역 카운터 동시 실행 충돌, 실제 DLT 전달 통합 검증 부재, 멱등성/정합성 시나리오 부재 |
| Redis | Cache-Aside, TTL, JSON 직렬화, 장애 시 DB fallback | eviction 실패 은폐, stampede, DB fallback 폭주, 쓰기-캐시 정합성 및 장애 테스트 부재 |
| AI Chat | 경력 원문 기반 제한, 짧은 답변 규칙, 입력 길이/rate limit, 오류 응답 분리 | 프롬프트만으로 사실성 보장, 출처 강제 없음, spoof 가능한 X-Forwarded-For, 비용/토큰/품질 관측 부재 |

## 3. 개선 백로그

| ID | 우선순위 | 상태 | 개선 항목 | 완료 조건 |
|---|---:|---|---|---|
| K-01 | P0 | 완료 | 원본/DLT partition 정합성 | 두 토픽의 partition 수가 같고 설정 테스트가 이를 검증한다. |
| K-02 | P0 | 완료 | Producer 발행 지표 정확성 | attempted/acknowledged/failed를 분리하고 Kafka send future 결과로 집계한다. |
| K-03 | P0 | 대기 | 실행 단위 격리 | runId별 통계를 유지하거나 동시에 하나의 실행만 허용한다. 이전 실행의 소비 결과가 새 실행에 섞이지 않는다. |
| K-04 | P0 | 대기 | Kafka 통합 테스트 | 세 원본 partition의 실패 레코드가 실제 DLT에 도착하고 retry 횟수와 DLT header를 검증한다. |
| K-05 | P1 | 대기 | 로컬/컨테이너 listener 분리 | 컨테이너 앱은 `kafka:9092`, 호스트 앱은 `localhost` listener를 사용한다. |
| K-06 | P1 | 대기 | 업무 정합성 시나리오 | Transactional Outbox와 eventId 기반 멱등 consumer를 장애/중복 테스트로 증명한다. |
| R-01 | P0 | 대기 | CacheErrorHandler 의미 분리 | 읽기 실패는 fail-open, clear/evict 실패는 호출자 또는 메트릭에 실패로 드러난다. |
| R-02 | P0 | 대기 | Redis 통합 테스트 | miss→DB→hit, TTL, 직렬화, Redis 중단 시 fallback을 Testcontainers로 검증한다. |
| R-03 | P1 | 대기 | stampede 및 fallback 보호 | 동시 요청 테스트와 DB bulkhead/rate limit으로 Redis 장애가 DB 장애로 전파되지 않음을 보인다. |
| R-04 | P1 | 대기 | 쓰기-캐시 정합성 | DB commit 이후 관련 category key를 무효화하며 실패 및 stale 허용 범위를 문서화한다. |
| R-05 | P2 | 대기 | 측정 품질 | cold miss, hit, DB warm, fallback을 분리하고 p50/p95/p99와 hit ratio를 노출한다. |
| C-01 | P0 | 대기 | 근거가 포함된 응답 계약 | 답변에 사용한 source id를 구조화해 반환하고 존재하지 않는 source를 거부한다. |
| C-02 | P0 | 대기 | 프롬프트 공격 회귀 테스트 | 규칙 무시, 시스템 프롬프트 유출, 허위 경력 생성, 민감 주제 질문의 기대 결과를 평가한다. |
| C-03 | P0 | 대기 | 신뢰 가능한 client 식별 | 임의 `X-Forwarded-For`를 신뢰하지 않고 trusted proxy/Cloudflare가 전달한 주소만 사용한다. |
| C-04 | P1 | 대기 | 토큰·비용·품질 관측 | latency, 성공/실패, input/output token, rate-limit 차단, grounded/abstain 비율을 Micrometer로 노출한다. |
| C-05 | P1 | 대기 | 검색 기반 컨텍스트 선택 | 전체 문서를 매번 넣는 대신 질문과 관련된 문서만 선택하고 근거가 없으면 답변을 보류한다. |

## 4. 개선 기록

### K-01 — 원본/DLT partition 정합성

#### 문제

`livelab.orders`는 3 partitions이지만 `livelab.orders.DLT`는 1 partition이었다.
Spring Kafka `DeadLetterPublishingRecoverer`의 기본 목적지는 원본 partition을 유지하므로,
원본 partition 1 또는 2에서 실패한 레코드는 1 partition DLT에 발행할 수 없다.

#### 결정

DLT도 원본과 같은 3 partitions으로 구성한다. 실패 레코드의 원본 partition 대응 관계가 유지되어
분석과 replay가 단순해지는 쪽을 선택했다.

#### 검증

- 설정 단위 테스트에서 원본과 DLT가 모두 3 partitions인지 확인한다.
- K-04에서 실제 Kafka를 사용해 모든 partition의 DLT 도착을 검증한다.

### K-02 — Producer 발행 지표 정확성

#### 문제

기존 `produced` 값은 Kafka 전송 전에 요청한 전체 건수를 더한 값이었다. `KafkaTemplate.send()`는
비동기인데 완료 결과를 확인하지 않았기 때문에 broker가 전송을 거부하거나 연결이 끊겨도 발행 성공처럼 보였다.

#### 결정

지표를 `attempted`, `acknowledged`, `publishFailed`로 분리한다. ACK와 실패는 send future의
완료 결과에서만 증가시키며, 동기적으로 발생하는 producer 예외도 실패로 집계한다.

#### 남은 한계

현재 통계는 여전히 애플리케이션 메모리에 있는 전역 값이다. 다른 사용자의 실행과 분리하는 작업은 K-03에서 다룬다.

## 5. AI 경력 Q&A 분석

### 현재 구조

애플리케이션 시작 시 `data/career`의 모든 Markdown을 하나의 system prompt로 만들고,
각 요청의 질문을 user message로 전달한다. 대화 이력은 저장하지 않는 단발성 Q&A다.

이 구조는 데이터가 작고 질문이 단순한 포트폴리오에는 비용 대비 합리적이다. 별도 vector database 없이도
전체 경력 데이터를 모델이 볼 수 있고, 서버에 대화나 개인정보를 장기 저장하지 않는 장점이 있다.

### 좋은 점

- 데이터에 없는 경험을 없다고 답하도록 명시했다.
- 조건, 정치, 종교 등 포트폴리오 밖 질문을 거절한다.
- 질문을 500자로 제한하고 IP별 호출 제한을 둔다.
- LLM의 4xx, 5xx, transient, network 오류를 사용자 메시지로 분리한다.
- 브라우저 출력에 `textContent`를 사용해 모델 출력에 의한 HTML/XSS를 차단한다.

### 중요한 한계

1. **프롬프트 규칙은 사실성 보장이 아니다.** 모델이 source에 없는 숫자나 파일명을 만들 수 있다.
2. **출처가 선택 사항이다.** 사용자가 답변의 근거를 기계적으로 확인할 수 없다.
3. **전체 컨텍스트 방식은 확장성이 낮다.** 문서가 늘수록 입력 토큰과 관련 없는 정보가 함께 증가한다.
4. **평가가 문자열 포함 테스트뿐이다.** 실제 질문에 대한 grounded answer, abstention, prompt injection 저항을 검증하지 않는다.
5. **`X-Forwarded-For`를 그대로 신뢰한다.** 직접 접근 가능한 환경에서는 헤더 위조로 rate limit을 우회할 수 있다.
6. **rate-limit bucket이 제거되지 않는다.** 장기간 다양한 IP가 접근하면 map이 계속 증가하며 다중 인스턴스 간 공유되지 않는다.
7. **질문 원문이 로그에 남는다.** 80자로 줄이지만 이메일·전화번호 같은 개인정보가 포함될 수 있다.
8. **화면 설명과 코드가 다르다.** 화면은 rate limit을 향후 개선처럼 설명하지만 이미 인메모리 IP 제한이 구현되어 있다.

### 권장 목표 구조

```text
Question
  → input policy / trusted client rate limit
  → career document selector
  → LLM with selected evidence
  → structured answer { answer, sources, grounded }
  → source validation
  → metrics and redacted audit log
```

초기에는 vector database를 바로 추가하지 않는다. 문서 수가 작으므로 파일별 키워드/태그 검색으로
관련 문서 2~4개만 선택하는 방식부터 적용하고, 검색 품질이 부족하다는 측정 결과가 있을 때 embedding을 도입한다.

### 챗봇 완료 기준

- 근거가 있으면 실제 존재하는 source id 1개 이상을 반환한다.
- 근거가 없으면 추측 대신 `grounded=false`와 정해진 보류 문구를 반환한다.
- 대표 질문과 공격 질문 데이터셋을 CI에서 평가한다.
- 질문 원문과 모델 응답 전문을 운영 로그에 남기지 않는다.
- 외부 API 장애, timeout, 429가 비용을 증폭시키지 않도록 retry budget을 검증한다.

## 6. 포트폴리오에서 보여줄 개선 형식

각 개선은 다음 순서로 DevLog에 남긴다.

1. 재현 가능한 증상
2. 실제 원인과 잘못된 초기 가설
3. 선택지와 trade-off
4. 최소 수정
5. 자동화된 회귀 테스트
6. 변경 전후 지표
7. 아직 남은 한계

이 형식을 지키면 기능의 개수보다 문제 해결 과정과 운영 판단을 더 강하게 증명할 수 있다.
