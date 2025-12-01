# Build stage
FROM gradle:8.5-jdk21 AS build

WORKDIR /app

# GitHub Packages 인증을 위한 ARG (RUN 직전에 선언하여 캐시 최적화)
ARG GITHUB_ACTOR
ARG GITHUB_TOKEN

# Gradle 설정 파일 복사
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle
COPY customer-api/build.gradle.kts ./customer-api/

# Gradle properties 파일에 인증 정보 저장 및 의존성 다운로드
RUN mkdir -p ~/.gradle && \
    echo "gpr.user=$GITHUB_ACTOR" >> ~/.gradle/gradle.properties && \
    echo "gpr.key=$GITHUB_TOKEN" >> ~/.gradle/gradle.properties && \
    gradle :customer-api:dependencies --no-daemon || true

# 소스 코드 복사
COPY customer-api/src ./customer-api/src

# 애플리케이션 빌드 (테스트는 CI에서 별도 실행)
RUN gradle :customer-api:bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy built jar from build stage
COPY --from=build /app/customer-api/build/libs/*.jar app.jar

# Expose application port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
