# Build stage
FROM gradle:8.5-jdk21 AS build

# GitHub Packages 인증을 위한 ARG
ARG GITHUB_ACTOR
ARG GITHUB_TOKEN

# 환경 변수로 설정 (Gradle이 사용)
ENV GITHUB_ACTOR=${GITHUB_ACTOR}
ENV GITHUB_TOKEN=${GITHUB_TOKEN}

WORKDIR /app

# Copy gradle configuration files
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle
COPY customer-api/build.gradle.kts ./customer-api/

# Download dependencies (cache layer)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY . .

# Build application (skip tests as they run in CI)
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
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
