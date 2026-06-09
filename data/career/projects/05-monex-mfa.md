<!-- DRAFT: needs user review -->
# MONEX 증권 MFA 인증 시스템

- 기간: 2021.03 ~ 2021.05
- 소속: 스마트아이엔지 → MONEX, Inc.
- 도메인: 증권 · 인증
- 현재 상태: MONEX 운영 중 (2021.09 공식 서비스화 후 지속 운영)

## 기술 스택
Java, Servlet Filter, MyBatis, SoftBank Message Link API, Google OTP, JSP

## 배경
실제 부정 로그인·비정상 출금 사고 발생 후 MFA 도입 필요. 10년 이상 운영된 레거시 JSP 시스템에 광범위한 영향 없이 보안 강화 요구.

## 핵심 기여
- **Servlet Filter 패턴**으로 기존 레거시 코드 수정 없이 **비침투적 MFA 통합**
- SMS(SoftBank Message Link API) · Email · Google OTP 3종 인증 수단 선택 구조 설계
- 초기 **5단계 인증 흐름 → 3단계 축소** 설계안 작성 및 고객사 설득·채택
- SMS 발송 실패 시 이메일 폴백 구조

## 결과
- SMS 발송 성공률 **99.2%**
- 도입 후 장애 **0건**
- **2021.09.27 MONEX 공식 출금 SMS 이중인증 서비스 론칭** (공식 페이지 확인 가능)
