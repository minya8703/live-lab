---
slug: grafana-cache-no-data
unit: 6
title: Grafana 의 "No data" 가 캐시 lazy 생성과 만났을 때
date: 2026-05-23
tags: [observability, spring-cache]
---

## 증상
Grafana 대시보드 5개 패널은 정상 동작, **"Spring Cache — Hits vs Misses"** 패널만 *"No data"*.
Prometheus 에서 `cache_gets_total` 메트릭 시리즈가 아예 존재하지 않음.

## 원인
Spring Boot 의 캐시 메트릭 자동 바인딩 (`CacheMetricsRegistrar`) 은
**`CacheManager` 가 시작 시점에 알고 있는 캐시**만 메트릭에 등록한다.

문제는 `RedisCacheManager` 가 캐시를 **lazy** 하게 만든다는 것 —
`@Cacheable("category-stats")` 가 처음 호출될 때야 비로소 캐시가 생성된다.

순서가 이렇다:
1. 앱 시작 → CacheManager 빈 등록 → 캐시 목록 = []
2. CacheMetricsRegistrar 가 빈 캐시 목록을 보고 메트릭 등록 안 함
3. 사용자가 /lab/redis.html 에서 첫 호출 → `category-stats` 캐시 lazy 생성
4. 캐시는 동작하지만 Micrometer 메트릭은 영원히 등록 안 됨

## 해결
캐시 이름을 시작 시점에 **명시 선언** + 통계 활성화:

```java
return RedisCacheManager.builder(factory)
    .cacheDefaults(config)
    .initialCacheNames(Set.of(ProductService.CACHE_NAME))
    .enableStatistics()
    .build();
```

`enableStatistics()` 도 필수 — 기본 OFF 상태에서는 `RedisCache` 가 hit/miss 카운터를
누적조차 안 한다.

## 일반화
**자동 와이어링과 lazy 생성이 만나면 메트릭이 조용히 사라진다.**
이런 류의 함정은 *에러 없이 침묵하는* 게 가장 무섭다.
Grafana 패널이 "No data" 일 때 의심할 것:

1. 메트릭 자체가 존재하는지 (Prometheus `/graph` 에서 메트릭 이름 검색)
2. 자동 등록되는 메트릭이 lazy 생성된 자원에 묶여있는지
3. 통계 수집 자체가 활성화돼있는지

## 교훈
관측성은 *"메트릭을 노출했다"* 가 끝이 아니라
*"의도한 메트릭이 실제로 흐른다"* 가 끝.
