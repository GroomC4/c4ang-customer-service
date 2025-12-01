# Dockerfile Migration Guide

## 개요

GitHub Actions workflow가 **JAR 사전 빌드 방식**으로 변경됨에 따라, 각 도메인 서비스의 Dockerfile도 이에 맞게 수정이 필요합니다.

## 변경 배경

### 이전 방식 (Dockerfile 내부 빌드)
```dockerfile
FROM gradle:8.5-jdk21 AS build
ARG GITHUB_ACTOR
ARG GITHUB_TOKEN
# ... Dockerfile 내에서 gradle bootJar 실행
```

**문제점:**
- 멀티 플랫폼 빌드 시 각 아키텍처마다 Gradle 빌드 반복 (AMD64 + ARM64)
- ARM64 빌드가 QEMU 에뮬레이션으로 8배 이상 느림
- GitHub Packages 인증 정보를 Docker build-args로 전달해야 함

### 변경된 방식 (JAR 사전 빌드)
```yaml
# GitHub Actions에서
- name: Pre-build JAR with Gradle
  run: ./gradlew bootJar -x test
  env:
    GITHUB_ACTOR: ${{ github.actor }}
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

- name: Build and push image
  uses: docker/build-push-action@v5
  with:
    build-args: |
      JAR_FILE=customer-api/build/libs/*.jar
```

**장점:**
- JAR 빌드는 1회만 수행 (GitHub Actions 네이티브 환경)
- Docker 빌드는 JAR 복사만 수행 (매우 빠름)
- 인증 정보가 Docker 이미지에 노출되지 않음

## 마이그레이션 방법

### 1. Dockerfile 수정

**Before:**
```dockerfile
# Build stage
FROM gradle:8.5-jdk21 AS build

ARG GITHUB_ACTOR
ARG GITHUB_TOKEN

WORKDIR /app

RUN mkdir -p /root/.gradle && \
    echo "gpr.user=$GITHUB_ACTOR" >> /root/.gradle/gradle.properties && \
    echo "gpr.key=$GITHUB_TOKEN" >> /root/.gradle/gradle.properties

COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle
COPY customer-api/build.gradle.kts ./customer-api/

RUN gradle :customer-api:dependencies --no-daemon || true

COPY customer-api/src ./customer-api/src

RUN gradle :customer-api:bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=build /app/customer-api/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**After:**
```dockerfile
# Runtime stage only - JAR is pre-built by GitHub Actions
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy pre-built JAR from GitHub Actions build
ARG JAR_FILE=customer-api/build/libs/*.jar
COPY ${JAR_FILE} app.jar

# Expose application port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 2. .dockerignore 수정

JAR 파일이 Docker 빌드 컨텍스트에 포함되도록 `.dockerignore`를 수정합니다.

```dockerignore
# Gradle
.gradle
# build/ 디렉토리는 JAR 복사를 위해 포함 (GitHub Actions에서 빌드된 JAR 사용)
!customer-api/build/libs/*.jar
```

> **중요**: `!customer-api/build/libs/*.jar` 부분의 경로는 각 서비스의 모듈 구조에 맞게 수정하세요.

### 3. JAR_FILE 경로 확인

각 서비스의 `JAR_FILE` 기본값은 해당 서비스의 빌드 결과물 경로와 일치해야 합니다.

| 서비스 | JAR 경로 |
|--------|----------|
| customer-service | `customer-api/build/libs/*.jar` |
| store-service | `store-api/build/libs/*.jar` |
| order-service | `order-api/build/libs/*.jar` |

## 로컬 테스트

변경 후 로컬에서 테스트:

```bash
# 1. JAR 빌드
./gradlew bootJar -x test

# 2. Docker 이미지 빌드
docker build -t my-service-test .

# 3. 컨테이너 실행 테스트
docker run --rm -p 8081:8081 my-service-test
```

## 체크리스트

- [ ] Dockerfile을 JAR 복사 방식으로 변경
- [ ] `.dockerignore`에서 JAR 파일 허용
- [ ] `JAR_FILE` ARG 기본값이 모듈 구조와 일치하는지 확인
- [ ] 로컬 Docker 빌드 테스트
- [ ] CI/CD 파이프라인 테스트
