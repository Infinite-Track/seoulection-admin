FROM eclipse-temurin:17-jdk AS builder

WORKDIR /workspace

# 1) 빌드 스크립트와 래퍼만 먼저 복사한다.
#    Docker는 어떤 레이어가 바뀌면 그 아래를 전부 다시 실행하므로, 소스보다 위에 둬야
#    아래 의존성 레이어가 살아남는다.
COPY gradlew .
COPY gradle ./gradle
COPY settings.gradle.kts build.gradle.kts ./
RUN chmod +x ./gradlew

# 2) 의존성을 이 레이어에 구워둔다. build.gradle.kts가 안 바뀌면 통째로 CACHED가 된다.
#    ⚠️ 캐시 마운트(--mount=type=cache)로 바꾸지 말 것 — 마운트 내용물은 레이어에 남지 않고
#       BuildKit의 캐시 exporter도 그것만은 내보내지 않아, GitHub Actions처럼 매번 새 러너인
#       환경에서는 효과가 0이 된다. 레이어에 굽는 방식만 로컬·CI 양쪽에서 통한다.
RUN ./gradlew resolveDependencies --no-daemon

# 3) 소스는 마지막. 여기가 바뀌어도 2)는 재사용되므로 컴파일만 다시 한다.
COPY src ./src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar app.jar

ENV SERVER_PORT=8081

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
