# Customer Service

고객(회원) 관리 및 인증을 담당하는 마이크로서비스입니다.

## 개요

e-commerce 플랫폼의 회원 관리 및 인증을 전담합니다. 일반 고객(Customer)과 판매자(Owner) 두 가지 사용자 유형을 지원하며, JWT 기반의 stateless 인증 체계를 사용합니다.

> **Note**: API 인증/인가는 Istio API Gateway에서 처리하며, 이 서비스는 토큰 발급 및 사용자 관리에 집중합니다.

## 인증 (Authentication)

### JWT 토큰 구조

RSA (RS256) 알고리즘 기반의 비대칭 키 서명을 사용합니다.

| 토큰 | 만료 시간 | 용도 |
|------|----------|------|
| Access Token | 5분 (dev) | API 요청 인증 |
| Refresh Token | 7일 | Access Token 갱신 |

### 토큰 Payload 구조
```json
{
  "sub": "user-id",
  "role": "CUSTOMER | OWNER",
  "iat": 1234567890,
  "exp": 1234567890,
  "iss": "ecommerce-service-api",
  "aud": "ecommerce-api"
}
```

### 인증 흐름

```
1. 클라이언트 → Customer Service: 로그인 요청
2. Customer Service → 클라이언트: JWT 토큰 발급 (Access + Refresh)
3. 클라이언트 → Istio Gateway: API 요청 (Authorization: Bearer <token>)
4. Gateway (RequestAuthentication): JWT 검증, 클레임 추출
5. Gateway (AuthorizationPolicy): Role 기반 접근 제어
6. Gateway → Backend Service: X-User-Id, X-User-Role 헤더 전달
```

### JWKS 엔드포인트

공개키는 JWKS (JSON Web Key Set) 형식으로 제공됩니다:
```
GET /.well-known/jwks.json
```

Istio RequestAuthentication이 이 엔드포인트에서 공개키를 가져와 JWT를 검증합니다.

## 주요 기능

- **회원 관리**: 고객/판매자 회원가입, 프로필 관리, Role 기반 권한 (CUSTOMER, OWNER)
- **인증**: JWT Access/Refresh Token 발급, BCrypt 비밀번호 암호화
- **내부 API**: 다른 서비스에서 사용자 정보 조회 (Internal API)

## 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Kotlin 2.0, JDK 21 |
| Framework | Spring Boot 3.3.4 |
| Database | PostgreSQL, Redis (Redisson) |
| Build | Gradle 8.5 |
| Test | JUnit 5, Kotest, Testcontainers, MockK |
| Docs | SpringDoc OpenAPI (Swagger UI) |

## 프로젝트 구조

```
customer-api/src/main/kotlin/com/groom/customer/
├── adapter/
│   ├── inbound/web/      # REST Controllers
│   └── outbound/
│       ├── persistence/  # JPA Repository
│       ├── security/     # JWT, Password Encoder
│       └── client/       # Feign Client (외부 서비스 호출)
├── application/
│   ├── dto/              # Command, Result DTO
│   └── service/          # Use Case 구현
├── domain/
│   ├── model/            # Entity, Value Object
│   ├── port/             # Port Interface (Hexagonal)
│   └── service/          # Domain Service
├── configuration/        # Spring 설정
└── common/               # 공통 유틸리티, 예외
```

### 아키텍처

**Hexagonal Architecture (Ports & Adapters)** 패턴을 따릅니다.

- `domain.port`: 외부 의존성에 대한 인터페이스 정의
- `adapter.outbound`: Port 인터페이스의 구현체

## 시작하기

### 사전 요구사항

- JDK 21
- Docker (로컬 인프라 실행용)

### 로컬 실행

```bash
# 인프라 실행 (PostgreSQL, Redis)
docker compose up -d

# 애플리케이션 실행
./gradlew :customer-api:bootRun

# API 문서 확인
open http://localhost:8081/swagger-ui.html
```

### 테스트

```bash
# 단위 테스트
./gradlew :customer-api:test

# 통합 테스트 (Testcontainers 사용)
./gradlew :customer-api:integrationTest
```

## API 엔드포인트

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/customers/signup` | 고객 회원가입 |
| POST | `/api/v1/auth/customers/login` | 고객 로그인 |
| POST | `/api/v1/auth/owners/signup` | 판매자 회원가입 |
| POST | `/api/v1/auth/owners/login` | 판매자 로그인 |
| POST | `/api/v1/auth/refresh` | Access Token 갱신 |
| GET | `/internal/users/{userId}` | 사용자 정보 조회 (내부용) |

> 상세 API 명세는 Swagger UI 참고

## 환경 변수

| 변수 | 설명 | 필수 |
|------|------|------|
| `JWT_PRIVATE_KEY` | JWT 서명용 RSA Private Key (PEM 형식) | O |
| `JWT_PUBLIC_KEY` | JWT 검증용 RSA Public Key (PEM 형식) | O |
| `SPRING_DATASOURCE_URL` | PostgreSQL URL | O |
| `SPRING_DATASOURCE_USERNAME` | DB 사용자명 | O |
| `SPRING_DATASOURCE_PASSWORD` | DB 비밀번호 | O |
| `SPRING_DATA_REDIS_HOST` | Redis 호스트 | O |

## CI/CD

- **CI**: PR 및 브랜치 푸시 시 자동 테스트
- **CD**: 태그 푸시 시 Docker 이미지 빌드 → AWS ECR 업로드

```bash
# 배포용 태그 생성
git tag v1.0.0
git push origin v1.0.0
```

## Contract Testing

Consumer-Driven Contract Testing을 위해 Spring Cloud Contract를 사용합니다.

```bash
# Contract Stub 생성 및 발행
./gradlew :customer-api:contractTest
./gradlew :customer-api:publish
```

Stub JAR은 `com.groom:customer-service-contract-stubs`로 GitHub Packages에 발행됩니다.

## 문의

@hayden-han
