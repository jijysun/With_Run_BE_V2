# ===== Build stage =====
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# 의존성 캐시 레이어: 빌드 스크립트만 먼저 복사
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# ===== Run stage =====
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd -r spring && useradd -r -g spring spring
COPY --from=build /app/build/libs/*.jar app.jar
USER spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
