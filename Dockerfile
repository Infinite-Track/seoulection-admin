FROM eclipse-temurin:17-jdk AS builder

WORKDIR /workspace

COPY gradlew .
COPY gradle ./gradle
COPY settings.gradle.kts build.gradle.kts ./
COPY src ./src

RUN chmod +x ./gradlew
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar app.jar

ENV SERVER_PORT=8081

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
