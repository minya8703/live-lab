---
slug: properties-encoding-bug
unit: 4
title: 한국어가 깨진 이유 — properties 가 아니라 Java 코드에 둬야 했다
date: 2026-05-22
tags: [encoding, debugging]
---

## 증상
Redis 데모 페이지의 카테고리 select 박스에서 한국어가 mangled 된 글자로 표시:

```
"???", "占쏙옙占쏙옙"
```

JSON 응답을 직접 봐도 깨져있고, DB의 `product.category` 컬럼도 깨진 채로 저장돼있다.

## 원인 추적
`application.properties` 의 `livelab.demo.categories=전자제품,도서,...` 라인이 문제.

Windows 환경에서 편집기/PowerShell이 파일을 **UTF-8이 아닌 인코딩**(CP949 또는 UTF-16)으로
저장하면 Spring Boot 가 잘못된 바이트로 문자열을 읽는다.

읽힌 mangled 문자열이 DataSeeder를 통해 DB에 그대로 insert.
이후 API가 그 mangled 문자열을 그대로 반환.
프론트엔드 UTF-8 환경에서 디코딩 시도 → 글자 깨짐.

## 시도한 해결
1. application.properties 인코딩 다시 저장 — 편집기마다 동작 다름, 재발 위험.
2. `spring.banner.charset` 같은 옵션 조정 — 의미 없음.
3. **Java 코드에 상수로 박기** — 채택.

## 채택한 패턴

```java
public final class DemoCategories {
    public static final List<String> ALL = List.of(
        "전자제품", "도서", "식료품", "의류", "스포츠"
    );
}
```

Java 소스 파일은 `maven-compiler-plugin` 이 `project.build.sourceEncoding=UTF-8` 을
강제하므로 어떤 편집기·터미널에서도 인코딩 사고가 일어나지 않는다.

`application.properties` 에서 카테고리 라인 제거. DB는 TRUNCATE 후 재시드.

## 일반화한 룰
**한국어/일본어 같은 비-ASCII 정적 데이터는 properties 가 아니라 Java 코드에 둔다.**

properties 는 *경계 자원* — 편집기·터미널·Maven 리소스 필터링 어디서든 한 번이라도
잘못된 인코딩으로 거치면 깨진다. Java 소스는 컴파일러가 인코딩을 강제 — 격리됨.

## 면접 답변용
> "한국어 카테고리를 properties에 두지 않고 별도 상수 클래스로 빼는 이유는,
> 운영 환경에서 한 번이라도 ANSI/CP949 인코딩으로 저장된 properties가 섞이면
> mangled 데이터가 DB까지 흘러가서 되돌리기 어려워서요. 비용 0의 보험입니다."

— 이 류의 작은 결정 하나가 시니어 면접관에게는 *"이 사람은 실전을 안다"* 신호다.
