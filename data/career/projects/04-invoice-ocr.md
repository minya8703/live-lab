<!-- DRAFT: needs user review -->
# Invoice OCR 자동 추출 시스템 (1인 개발)

- 기간: 2024.10 ~ 2026.03
- 소속: NARINER (→ 한샘)
- 도메인: 해외 거래처 인보이스 자동화
- 진행 방식: 1인 개발 + AI 코딩 도구(Claude Code) 활용

## 기술 스택
Python, FastAPI, PyMuPDF, PaddleOCR, EasyOCR, MariaDB, React + Konva.js

## 핵심 기여
- PyMuPDF 텍스트 추출 → 회사명·금액 표기 정규화 → 회사별 추출 규칙 매핑 **3단계 파이프라인** 설계
- 스캔 PDF(텍스트 레이어 없음) 감지 시 **PaddleOCR → EasyOCR 자동 폴백** 구조
- Strategy + Template Method 패턴으로 회사별 추출기 분리 (신규 거래처 = 파일 1개 + 등록 1줄)
- KMeans/DBSCAN 클러스터링 기반 좌표 학습 시스템
- FastAPI 38개 REST API + React/Konva.js 좌표 UI
- 비개발자(현업)가 직접 추출 좌표를 설정할 수 있는 웹 UI (운영 자립성)

## 결과
- 처리 시간 **5분 → 수초** (건당)
- 거래처 **12개** 자동 처리
- 자동 추출 필드 **18개**
- CPU 환경(GPU 없음)에서 동작
