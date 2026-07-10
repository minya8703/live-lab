# 멀티 스테이지 빌드 — 빌드 환경(JDK 21)과 런타임(JRE 21)을 분리해 최종 이미지를 가볍게.
# eclipse-temurin 은 multi-arch (amd64 + arm64) — t4g.small(ARM) 에서 그대로 동작.

# ===== Stage 1: Build =====
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Gradle wrapper + 빌드 스크립트 먼저 복사 (의존성 캐싱)
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle/
RUN chmod +x gradlew

# 의존성 다운로드 (소스 변경 없으면 캐시 재사용)
RUN ./gradlew dependencies --no-daemon || true

# 소스 복사
COPY src src/
COPY data data/

# Cache-busting: git hash를 CSS/JS 참조에 쿼리스트링으로 주입
# → URL이 배포마다 바뀌므로 CDN/브라우저 캐시가 자동 무효화됨
ARG BUILD_VERSION=dev
RUN find src/main/resources/static -name '*.html' \
    -exec sed -i -E "s/\.(css|js)(\")/.\1?v=${BUILD_VERSION}\2/g" {} +

# 빌드
RUN ./gradlew bootJar --no-daemon -x test

# ===== Stage 2: Runtime =====
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# JVM 옵션 — 컨테이너 메모리 인식 + UTF-8 강제
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8"

# 빌드된 JAR 만 복사 (소스·gradle 디렉토리 제외 — 이미지 크기 최소화)
COPY --from=build /workspace/build/libs/*.jar app.jar

# 런타임에 data/career/ 와 data/devlog/ 마크다운이 필요하므로 같이 복사
COPY --from=build /workspace/data data/

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
