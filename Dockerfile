# Build stage
FROM gradle:8.5-jdk21 AS build

# GitHub Packages 인증을 위한 ARG
ARG GITHUB_ACTOR
ARG GITHUB_TOKEN

WORKDIR /app

# Gradle properties 파일에 인증 정보 저장 (ENV 대신)
RUN mkdir -p ~/.gradle && \
    echo "gpr.user=${GITHUB_ACTOR}" >> ~/.gradle/gradle.properties && \
    echo "gpr.key=${GITHUB_TOKEN}" >> ~/.gradle/gradle.properties

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
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
