# Career Data — 챗봇 컨텍스트의 단일 진실원

이 디렉토리의 모든 `.md` 파일이 AI 경력 Q&A 챗봇(U3)의 컨텍스트로 주입된다.
챗봇은 이 데이터 **밖의 사실을 절대 답하지 않는다**. 데이터에 없으면 "없다"고 답한다.

## 구조

```
data/career/
├── profile.md              ← 1줄 포지셔닝, 연차, 컨택트
├── summary.md              ← 챗봇이 가장 먼저 참조할 한 페이지 요약
├── philosophy.md           ← 일하는 방식, 의사결정 원칙
├── tech-stack.md           ← 실제 사용한 기술과 깊이
├── gaps-and-direction.md   ← 못해본 영역 + 현재 학습·적용 중인 것 (전략적 핵심)
├── meta-this-project.md    ← 이 사이트(Live Lab) 자체에 대한 답변 가이드
├── faq.md                  ← 자주 묻는 질문과 모범 답안
├── experience/             ← 회사·기간별 (시간순 역행)
└── projects/               ← 프로젝트별 (시간순 역행)
```

## 작성 원칙

1. **사실만**: 날짜·수치는 검증된 것만. 추정·과장 금지.
2. **구조화된 사실**: 자유 산문보다 `- 키: 값` 형태가 RAG에 유리.
3. **수치 우선**: "많이", "오래" 같은 표현 금지. 가능하면 숫자.
4. **부족함도 자산**: 못한 것을 숨기지 않고 [gaps-and-direction.md](gaps-and-direction.md)에 명시.
5. **변경 이력**: 중요 갱신은 파일 하단에 `<!-- Updated: YYYY-MM-DD -->` 주석.

## DRAFT 표기

초기 파일은 기존 [minya8703.github.io](https://minya8703.github.io/) 데이터를 1회 스크레이핑하여 채워졌다.
사용자 검토 전 모든 파일 상단에 `<!-- DRAFT: needs user review -->` 가 있다. 검토 후 해당 줄을 제거한다.
