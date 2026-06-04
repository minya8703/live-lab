<!-- DRAFT: needs user review -->
# Invoice OCR 자동 추출 시스템

- 기간: 2024.10 ~ 2026.03
- 소속: NARINER
- 도메인: 거래처 청구서 자동화

## 기술 스택
Python, PyMuPDF, PaddleOCR, EasyOCR, 웹 기반 좌표 설정 UI

## 핵심 기여
- PyMuPDF 텍스트 추출 → 정규화 → 규칙 매핑 3단계 파이프라인 설계
- 스캔 PDF 감지 시 **PaddleOCR → EasyOCR 자동 폴백** 구조
- 비개발자(현업)가 직접 추출 좌표를 설정할 수 있는 웹 UI

## 결과
- 거래처 **12개** 자동 처리
- 자동 추출 필드 **18개**
