# Customer Service

고객(회원) 관리 및 인증을 담당하는 마이크로서비스입니다.

## 개요

이 서비스는 e-commerce 플랫폼의 회원 관리 및 인증/인가를 전담합니다. 일반 고객(Customer)과 판매자(Owner) 두 가지 사용자 유형을 지원하며, JWT 기반의 stateless 인증 체계를 사용합니다.

## 주요 기능

### 회원 관리
- 일반 고객 및 판매자 회원가입
- 이메일 기반 중복 가입 방지
- 사용자 프로필 관리
- Role 기반 권한 관리 (CUSTOMER, OWNER)

### 인증/인가
- JWT 기반 Access Token 발급 (5분 유효)
- Refresh Token을 통한 토큰 갱신 (3일 유효)
- BCrypt 기반 비밀번호 암호화
- Spring Security를 활용한 API 인증/인가
- 로그인/로그아웃 처리

## 기술 스택

### Core
- **Language**: Kotlin 2.2.20
- **Framework**: Spring Boot 3.3.4
- **JDK**: OpenJDK 21
- **Build Tool**: Gradle 8.14

### Database
- **Primary DB**: PostgreSQL
- **Cache**: Redis (Redisson)
- **ORM**: Spring Data JPA + Hibernate

### Security
- **Authentication**: JWT (java-jwt 4.4.0)
- **Password Encryption**: BCrypt
- **Security Framework**: Spring Security

### Cloud & Communication
- **HTTP Client**: Spring Cloud OpenFeign
- **API Documentation**: SpringDoc OpenAPI (Swagger UI)

### Testing
- **Testing Framework**: JUnit 5, Kotest
- **Integration Test**: Testcontainers, Docker Compose
- **Mocking**: MockK

### CI/CD
- **CI**: GitHub Actions
- **Container Registry**: AWS ECR
- **Container Platform**: Docker

## 프로젝트 구조

```
c4ang-customer-service/
├── customer-api/                    # 메인 애플리케이션 모듈
│   ├── src/main/kotlin/
│   │   └── com/groom/customer/
│   │       ├── CustomerApiApplication.kt
│   │       ├── application/         # 애플리케이션 서비스 계층
│   │       │   ├── dto/            # Command, Result DTO
│   │       │   └── service/        # 유스케이스 구현
│   │       ├── domain/             # 도메인 모델
│   │       │   ├── model/          # 엔티티, 값 객체
│   │       │   └── repository/     # 저장소 인터페이스
│   │       ├── inbound/            # 인바운드 어댑터 (API)
│   │       │   └── web/            # REST Controller
│   │       ├── outbound/           # 아웃바운드 어댑터
│   │       │   ├── persistence/    # JPA Repository 구현
│   │       │   └── cache/          # Redis 구현
│   │       ├── security/           # 보안 설정
│   │       │   ├── jwt/            # JWT 인증
│   │       │   └── filter/         # Security Filter
│   │       ├── configuration/      # Spring 설정
│   │       └── common/             # 공통 유틸리티
│   │           └── exception/      # 예외 처리
│   └── src/test/kotlin/            # 테스트 코드
│       └── com/groom/customer/
│           ├── application/        # 서비스 레이어 테스트
│           ├── inbound/web/        # 컨트롤러 통합 테스트
│           └── common/             # 테스트 공통 설정
│
├── c4ang-platform-core/            # 서브모듈: 테스트 인프라
│   ├── testcontainers/             # Testcontainers 설정
│   └── docker-compose/             # 통합 테스트용 Docker Compose
│
├── c4ang-infra/                    # 서브모듈: K8s 배포 설정
│   └── helm/                       # Helm Charts
│
├── build.gradle.kts                # Gradle 빌드 스크립트
├── Dockerfile                      # 컨테이너 이미지 빌드
└── .github/workflows/              # CI/CD 파이프라인
    └── ci.yml
```

### 아키텍처 패턴

이 프로젝트는 **Hexagonal Architecture (Ports & Adapters)** 패턴을 따릅니다:

- **domain**: 비즈니스 로직과 도메인 모델
- **application**: 유스케이스 구현 (비즈니스 흐름 조율)
- **inbound**: 외부에서 들어오는 요청 처리 (REST API)
- **outbound**: 외부 시스템과의 통신 (DB, Cache, 외부 API)

## 시작하기

### 사전 요구사항

- JDK 21
- Docker & Docker Compose (로컬 개발용)
- Gradle 8.14+ (또는 gradlew 사용)

### 로컬 실행

1. **저장소 클론 (서브모듈 포함)**
```bash
git clone --recursive https://github.com/your-org/c4ang-customer-service.git
cd c4ang-customer-service
```

2. **서브모듈 업데이트 (이미 클론한 경우)**
```bash
git submodule update --init --recursive
```

3. **인프라 시작 (PostgreSQL, Redis)**
```bash
docker compose up -d
```

4. **애플리케이션 실행**
```bash
./gradlew :customer-api:bootRun
```

5. **API 문서 확인**
```
http://localhost:8080/swagger-ui.html
```

### 테스트 실행

#### 단위 테스트
```bash
./gradlew :customer-api:test
```

#### 통합 테스트
```bash
./gradlew :customer-api:integrationTest
```

통합 테스트는 Testcontainers를 사용하여 실제 PostgreSQL과 Redis 컨테이너를 띄워 실행합니다.

## API 엔드포인트

### 일반 고객 (Customer)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/customers/signup` | 일반 고객 회원가입 |
| POST | `/api/v1/auth/customers/login` | 일반 고객 로그인 |
| POST | `/api/v1/auth/customers/logout` | 일반 고객 로그아웃 |

### 판매자 (Owner)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/owners/signup` | 판매자 회원가입 |
| POST | `/api/v1/auth/owners/login` | 판매자 로그인 |
| POST | `/api/v1/auth/owners/logout` | 판매자 로그아웃 |

### 토큰 관리

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/refresh` | Access Token 갱신 |

자세한 API 명세는 실행 후 Swagger UI를 참고하세요.

## 환경 설정

### 주요 설정 (application.yml)

```yaml
spring:
  application:
    name: customer-api
  datasource:
    # PostgreSQL 설정
  jpa:
    hibernate:
      ddl-auto: none
  redis:
    # Redis 설정

security:
  jwt:
    secret: ${JWT_SECRET}
    issuer: ecommerce-service-api
    accessTokenExpirationMinutes: 5
    refreshTokenExpirationDays: 3

encryption:
  encoder:
    bcrypt-strength: 10
```

### 환경 변수

- `JWT_SECRET`: JWT 서명 키 (필수)
- `SPRING_DATASOURCE_URL`: PostgreSQL 연결 URL
- `SPRING_DATASOURCE_USERNAME`: DB 사용자명
- `SPRING_DATASOURCE_PASSWORD`: DB 비밀번호
- `SPRING_DATA_REDIS_HOST`: Redis 호스트
- `SPRING_DATA_REDIS_PORT`: Redis 포트

## CI/CD

### GitHub Actions 워크플로우

- **CI**: Pull Request 및 브랜치 푸시 시 자동 테스트 실행
- **CD**: 태그 푸시 시 Docker 이미지 빌드 및 ECR 업로드

```bash
# 배포용 태그 생성
git tag v1.0.0
git push origin v1.0.0
```

## 서브모듈 관리

### c4ang-platform-core
통합 테스트를 위한 공통 인프라 설정을 포함합니다.
- Testcontainers 기본 설정
- Docker Compose 파일 (PostgreSQL, Redis)

### c4ang-infra
K8s 배포를 위한 Helm Charts와 인프라 설정을 포함합니다.

### 서브모듈 업데이트
```bash
git submodule update --remote
```

## 개발 가이드

### 코드 스타일
- Kotlin 공식 코딩 컨벤션 준수
- ktlint 자동 포맷팅 적용

```bash
./gradlew ktlintFormat
```

### 브랜치 전략
- `main`: 프로덕션 배포 브랜치
- `develop`: 개발 통합 브랜치
- `feature/*`: 기능 개발 브랜치
- `release/*`: 릴리스 준비 브랜치

## 문의
contract @hayden-han
