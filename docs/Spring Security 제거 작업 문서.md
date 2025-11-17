# Spring Security 제거 작업 문서

## 📋 작업 개요

**작업 일자**: 2025-11-13<br>
**작업자**: @hayden-han<br>
**작업 목적**: Istio API Gateway로 인증/인가 책임을 이관함에 따라 Customer Service 내 불필요한 Spring Security 인증/인가 로직 제거

## 🎯 배경 및 목적

### 현재 아키텍처
- **기존 (모놀리식 잔재)**: Customer Service가 JWT 검증 + 인가 처리
- **새로운 (MSA)**: Istio API Gateway가 JWT 검증 + 인가 처리

### Customer Service의 새로운 책임
이 서비스는 **인증 토큰 발급 및 관리**만 담당합니다:
1. ✅ 회원가입
2. ✅ 로그인 (JWT Access/Refresh Token 발급)
3. ✅ 로그아웃 (Refresh Token 무효화)
4. ✅ 토큰 갱신 (Refresh Token → 새로운 Access Token)

### 제거 대상
- Spring Security 인증/인가 설정
- JWT 검증 필터 (Istio가 처리)
- SecurityContext 기반 사용자 정보 추출 로직

---

## 📂 작업 범위

### 1. 제거할 파일 (7개)

#### 1.1 Security Configuration
```
customer-api/src/main/kotlin/com/groom/customer/configuration/security/
├── SecurityConfig.kt                      # Spring Security 설정
├── JwtAuthenticationFilter.kt             # JWT 검증 필터
├── CustomAuthenticationEntryPoint.kt      # 401 에러 핸들러
└── CustomAccessDeniedHandler.kt           # 403 에러 핸들러
```

**제거 이유**: Istio가 모든 인증/인가를 처리하므로 불필요

#### 1.2 Authentication Context
```
customer-api/src/main/kotlin/com/groom/customer/security/
└── AuthenticationContext.kt               # SecurityContext 사용자 정보 추출
```

**제거 이유**: SecurityContext를 사용하지 않으므로 불필요

### 2. 수정할 파일 (5개)

#### 2.1 JWT Token Provider
**파일**: `customer-api/src/main/kotlin/com/groom/customer/security/jwt/JwtTokenProvider.kt`

**변경 사항**:
- ✅ **유지**: `generateAccessToken()` - Access Token 발급
- ✅ **유지**: `generateRefreshToken()` - Refresh Token 발급
- ❌ **제거**: `validateToken()` - JWT 검증 (Istio가 처리)
- ❌ **제거**: `verifyTokenSignature()` - 서명 검증
- ❌ **제거**: `validateTokenHeader()` - 헤더 검증
- ❌ **제거**: `validateRequiredClaims()` - 클레임 검증
- ❌ **제거**: `extractAuthorizationData()` - 인증 데이터 추출

**수정 후**:
```kotlin
@Component
class JwtTokenProvider(
    private val properties: JwtProperties,
    private val clock: Clock,
) {
    private val algorithm: Algorithm = Algorithm.HMAC256(properties.secret)

    fun generateAccessToken(data: AuthorizationData): String { ... }

    fun generateRefreshToken(data: AuthorizationData): String { ... }

    // validateToken 관련 모든 메서드 제거
}
```

#### 2.2 Customer Authentication Controller
**파일**: `customer-api/src/main/kotlin/com/groom/customer/inbound/web/CustomerAuthenticationController.kt`

**변경 사항**: Logout 메서드 수정
```kotlin
// 변경 전 (line 97-103)
@PostMapping("/logout")
@ResponseStatus(HttpStatus.NO_CONTENT)
fun logout() {
    val userId = authenticationContext.getCurrentUserId()
    customerAuthenticationService.logout(LogoutCommand(userId))
}

// 변경 후
@PostMapping("/logout")
@ResponseStatus(HttpStatus.NO_CONTENT)
fun logout(@RequestHeader("X-User-Id") userIdHeader: String) {
    val userId = UUID.fromString(userIdHeader)
    customerAuthenticationService.logout(LogoutCommand(userId))
}
```

**주요 변경점**:
- SecurityContext 대신 Istio가 주입한 HTTP 헤더에서 userId 추출
- `authenticationContext` 의존성 제거

#### 2.3 Owner Authentication Controller
**파일**: `customer-api/src/main/kotlin/com/groom/customer/inbound/web/OwnerAuthenticationController.kt`

**변경 사항**: Customer Controller와 동일하게 Logout 메서드 수정

```kotlin
// 변경 전 (line 97-103)
@PostMapping("/logout")
@ResponseStatus(HttpStatus.NO_CONTENT)
fun logout() {
    val userId = authenticationContext.getCurrentUserId()
    ownerAuthenticationService.logout(LogoutCommand(userId))
}

// 변경 후
@PostMapping("/logout")
@ResponseStatus(HttpStatus.NO_CONTENT)
fun logout(@RequestHeader("X-User-Id") userIdHeader: String) {
    val userId = UUID.fromString(userIdHeader)
    ownerAuthenticationService.logout(LogoutCommand(userId))
}
```

#### 2.4 Build Configuration
**파일**: `customer-api/build.gradle.kts`

**변경 사항**: Spring Security 의존성 제거

```kotlin
dependencies {
    // ❌ 제거
    // implementation("org.springframework.boot:spring-boot-starter-security")

    // ❌ 제거 (테스트)
    // testImplementation("org.springframework.security:spring-security-test")

    // 나머지 의존성은 유지
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.auth0:java-jwt:4.4.0")
    // ...
}
```

#### 2.5 Application Configuration
**파일**: `customer-api/src/main/resources/application.yml`

**변경 사항**: 불필요한 Security 로그 설정 제거 (있는 경우)

```yaml
# 제거 대상 (있는 경우)
logging:
  level:
    org.springframework.security: INFO  # 제거
```

### 3. 신규 생성 파일 (1개) - Optional

#### 3.1 Istio Header Extractor
**파일**: `customer-api/src/main/kotlin/com/groom/customer/common/util/IstioHeaderExtractor.kt`

**목적**: Istio가 주입한 헤더 추출 로직 중앙화 (옵션)

```kotlin
package com.groom.customer.common.util

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Istio API Gateway가 주입한 인증 헤더를 추출하는 유틸리티
 */
@Component
class IstioHeaderExtractor {
    companion object {
        const val USER_ID_HEADER = "X-User-Id"
        const val USER_ROLE_HEADER = "X-User-Role"
    }

    /**
     * Istio가 JWT 검증 후 주입한 사용자 ID를 추출합니다.
     *
     * @throws IllegalStateException 헤더가 없거나 형식이 잘못된 경우
     */
    fun extractUserId(request: HttpServletRequest): UUID {
        val userId = request.getHeader(USER_ID_HEADER)
            ?: throw IllegalStateException("$USER_ID_HEADER header not found. Istio authentication failed.")

        return try {
            UUID.fromString(userId)
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException("Invalid user ID format in $USER_ID_HEADER: $userId", e)
        }
    }

    /**
     * Istio가 JWT 검증 후 주입한 사용자 역할을 추출합니다.
     */
    fun extractUserRole(request: HttpServletRequest): String {
        return request.getHeader(USER_ROLE_HEADER)
            ?: throw IllegalStateException("$USER_ROLE_HEADER header not found. Istio authentication failed.")
    }
}
```

**사용 예시** (Controller에서):
```kotlin
@PostMapping("/logout")
fun logout(request: HttpServletRequest) {
    val userId = istioHeaderExtractor.extractUserId(request)
    customerAuthenticationService.logout(LogoutCommand(userId))
}
```

---

## 🧪 테스트 계획

### 1. 단위 테스트 수정

#### 1.1 제거할 테스트
- `JwtAuthenticationFilterTest` (존재 시)
- `SecurityConfigTest` (존재 시)
- `AuthenticationContextTest` (존재 시)

#### 1.2 수정할 테스트
- `JwtTokenProviderTest`: 검증 로직 테스트 제거, 발급 로직 테스트만 유지
- `CustomerAuthenticationControllerTest`: Logout 테스트에서 SecurityContext Mock → 헤더 Mock으로 변경
- `OwnerAuthenticationControllerTest`: 동일하게 수정

**테스트 수정 예시**:
```kotlin
// 변경 전
@Test
fun `로그아웃 성공`() {
    // given
    val userId = UUID.randomUUID()
    every { authenticationContext.getCurrentUserId() } returns userId

    // when & then
    mockMvc.perform(post("/api/v1/auth/customers/logout")
        .header("Authorization", "Bearer valid-token"))
        .andExpect(status().isNoContent)
}

// 변경 후
@Test
fun `로그아웃 성공`() {
    // given
    val userId = UUID.randomUUID()

    // when & then
    mockMvc.perform(post("/api/v1/auth/customers/logout")
        .header("X-User-Id", userId.toString()))
        .andExpect(status().isNoContent)
}
```

### 2. 통합 테스트 수정

#### 2.1 수정 필요 파일
- `CustomerAuthenticationControllerIntegrationTest.kt`
- `OwnerAuthenticationControllerIntegrationTest.kt`
- `TokenRefreshControllerIntegrationTest.kt`

**주요 변경점**:
- JWT 토큰 생성 로직 제거 (테스트에서 직접 생성하던 부분)
- Istio 헤더를 직접 주입하는 방식으로 변경
- `@WithMockUser` 등 Spring Security 테스트 어노테이션 제거

**통합 테스트 수정 예시**:
```kotlin
// 변경 전
@Test
fun `로그인한 사용자가 로그아웃하면 204 No Content를 반환한다`() {
    // given
    val user = createTestUser()
    val accessToken = jwtTokenProvider.generateAccessToken(
        AuthorizationData(user.id.toString(), user.role.name)
    )

    // when & then
    mockMvc.perform(
        post("/api/v1/auth/customers/logout")
            .header("Authorization", "Bearer $accessToken")
    ).andExpect(status().isNoContent)
}

// 변경 후
@Test
fun `로그인한 사용자가 로그아웃하면 204 No Content를 반환한다`() {
    // given
    val user = createTestUser()

    // when & then
    mockMvc.perform(
        post("/api/v1/auth/customers/logout")
            .header("X-User-Id", user.id.toString())
    ).andExpect(status().isNoContent)
}
```

### 3. 테스트 실행 계획
```bash
# 1. 단위 테스트
./gradlew :customer-api:test

# 2. 통합 테스트
./gradlew :customer-api:integrationTest

# 3. 전체 빌드
./gradlew clean build
```

---

## 🔄 Istio 설정 요구사항

### 1. RequestAuthentication 설정

Customer Service로 들어오는 요청에 대해 JWT 검증을 수행해야 합니다:

```yaml
apiVersion: security.istio.io/v1beta1
kind: RequestAuthentication
metadata:
  name: customer-service-jwt
  namespace: default
spec:
  selector:
    matchLabels:
      app: customer-service
  jwtRules:
  - issuer: "ecommerce-service-api"  # application.yml의 security.jwt.issuer와 동일
    jwksUri: "http://customer-service:8080/.well-known/jwks.json"  # 또는 외부 JWKS URI
    # 또는 로컬 JWKS 사용:
    # jwks: |
    #   {"keys":[...]}
    outputPayloadToHeader: "x-jwt-payload"
    forwardOriginalToken: true
```

### 2. AuthorizationPolicy 설정

인가 규칙을 정의합니다:

```yaml
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: customer-service-authz
  namespace: default
spec:
  selector:
    matchLabels:
      app: customer-service
  action: CUSTOM
  provider:
    name: "jwt-claim-mapper"
  rules:
  # 인증 불필요 경로
  - to:
    - operation:
        paths:
        - "/actuator/health"
        - "/swagger-ui/*"
        - "/v3/api-docs/*"
        - "/api/v1/auth/customers/signup"
        - "/api/v1/auth/customers/login"
        - "/api/v1/auth/owners/signup"
        - "/api/v1/auth/owners/login"
        - "/api/v1/auth/refresh"
    when:
    - key: request.auth.claims[iss]
      notValues: ["*"]  # 토큰 없어도 허용

  # Customer 로그아웃 - CUSTOMER 역할 필요
  - to:
    - operation:
        paths: ["/api/v1/auth/customers/logout"]
        methods: ["POST"]
    when:
    - key: request.auth.claims[role]
      values: ["CUSTOMER"]

  # Owner 로그아웃 - OWNER 역할 필요
  - to:
    - operation:
        paths: ["/api/v1/auth/owners/logout"]
        methods: ["POST"]
    when:
    - key: request.auth.claims[role]
      values: ["OWNER"]
```

### 3. EnvoyFilter - JWT Claims를 HTTP 헤더로 변환

```yaml
apiVersion: networking.istio.io/v1alpha3
kind: EnvoyFilter
metadata:
  name: jwt-claims-to-headers
  namespace: default
spec:
  workloadSelector:
    labels:
      app: customer-service
  configPatches:
  - applyTo: HTTP_FILTER
    match:
      context: SIDECAR_INBOUND
      listener:
        filterChain:
          filter:
            name: "envoy.filters.network.http_connection_manager"
            subFilter:
              name: "envoy.filters.http.jwt_authn"
    patch:
      operation: INSERT_AFTER
      value:
        name: envoy.filters.http.lua
        typed_config:
          "@type": type.googleapis.com/envoy.extensions.filters.http.lua.v3.Lua
          inline_code: |
            function envoy_on_request(request_handle)
              local jwt_payload = request_handle:headers():get("x-jwt-payload")
              if jwt_payload then
                local json = require("cjson")
                local payload = json.decode(jwt_payload)

                -- JWT의 sub(subject) claim을 X-User-Id 헤더로
                if payload.sub then
                  request_handle:headers():add("X-User-Id", payload.sub)
                end

                -- JWT의 role claim을 X-User-Role 헤더로
                if payload.role then
                  request_handle:headers():add("X-User-Role", payload.role)
                end
              end
            end
```

---

## ⚠️ 주의사항 및 위험 요소

### 1. Istio 헤더 이름 확인 필수
- 위 문서에서는 `X-User-Id`, `X-User-Role`을 사용했지만
- 실제 Istio 설정에 따라 헤더 이름이 다를 수 있음
- **반드시 인프라팀과 헤더 이름 협의 필요**

### 2. 로컬 개발 환경
로컬에서 Istio 없이 개발할 경우:

**방법 1: 개발용 필터 추가**
```kotlin
@Profile("local")
@Component
class LocalDevAuthFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // 로컬 개발용 Mock 헤더 주입
        if (request.getHeader("X-User-Id") == null) {
            val wrapper = object : HttpServletRequestWrapper(request) {
                override fun getHeader(name: String): String? {
                    return when (name) {
                        "X-User-Id" -> "00000000-0000-0000-0000-000000000001"
                        "X-User-Role" -> "CUSTOMER"
                        else -> super.getHeader(name)
                    }
                }
            }
            filterChain.doFilter(wrapper, response)
            return
        }
        filterChain.doFilter(request, response)
    }
}
```

**방법 2: Postman/Curl 요청 시 헤더 직접 추가**
```bash
curl -X POST http://localhost:8080/api/v1/auth/customers/logout \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000"
```

### 3. 헤더 검증 누락 위험
- Istio가 정상 작동하지 않으면 헤더가 주입되지 않음
- 헤더 없을 시 500 에러 대신 명확한 401/403 에러 반환 필요
- 헤더 검증 로직을 `IstioHeaderExtractor`에 집중

### 4. 보안 고려사항
- ⚠️ **중요**: 이 서비스는 Istio 뒤에서만 실행되어야 함
- Istio를 우회하는 직접 접근을 막기 위해 NetworkPolicy 설정 필요:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: customer-service-ingress
spec:
  podSelector:
    matchLabels:
      app: customer-service
  policyTypes:
  - Ingress
  ingress:
  # Istio Ingress Gateway에서만 접근 허용
  - from:
    - namespaceSelector:
        matchLabels:
          name: istio-system
    - podSelector:
        matchLabels:
          app: istio-ingressgateway
  # 같은 네임스페이스 내 다른 서비스 접근 허용 (옵션)
  - from:
    - podSelector: {}
```

### 5. 에러 처리
Istio 헤더가 없거나 잘못된 경우 명확한 에러 메시지:

```kotlin
@RestControllerAdvice
class IstioHeaderExceptionHandler {

    @ExceptionHandler(IllegalStateException::class)
    fun handleMissingIstioHeader(e: IllegalStateException): ResponseEntity<ErrorResponse> {
        return if (e.message?.contains("header") == true) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponse(
                    code = "MISSING_AUTH_HEADER",
                    message = "Authentication failed. Request must pass through API Gateway.",
                    timestamp = Instant.now()
                )
            )
        } else {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponse(
                    code = "INTERNAL_ERROR",
                    message = e.message ?: "Internal server error",
                    timestamp = Instant.now()
                )
            )
        }
    }
}
```

---

## 📝 롤백 계획

작업 중 문제 발생 시 신속한 롤백을 위한 계획:

### 1. Git을 통한 롤백
```bash
# 현재 브랜치에서 작업 전 커밋으로 되돌리기
git log --oneline -10  # 작업 전 커밋 해시 확인
git reset --hard <commit-hash>

# 또는 브랜치 전체 초기화
git fetch origin
git reset --hard origin/main
```

### 2. 단계별 롤백 우선순위

**Phase 1 실패 시** (파일 제거):
- Git revert로 삭제한 파일 복구
- 가장 안전한 시점

**Phase 2 실패 시** (코드 수정):
- 수정한 파일만 선택적으로 revert
- 테스트 실패 시 즉시 중단

**Phase 3 실패 시** (의존성 제거):
- `build.gradle.kts` 복구
- 의존성 재다운로드: `./gradlew clean build --refresh-dependencies`

**Phase 4 실패 시** (배포 후):
- K8s에서 이전 버전 이미지로 롤백
- `kubectl rollout undo deployment/customer-service`

### 3. 긴급 복구 시나리오

**시나리오 1: 로그아웃 API 작동 안 함**
- 원인: Istio 헤더 이름 불일치
- 조치: Controller에서 하드코딩된 헤더 이름 수정
- 예상 복구 시간: 10분

**시나리오 2: 모든 API 403 에러**
- 원인: Istio AuthorizationPolicy 설정 오류
- 조치: Istio 설정을 일시적으로 ALLOW_ALL로 변경
- 예상 복구 시간: 5분

**시나리오 3: 통합 테스트 실패**
- 원인: 테스트 코드 수정 누락
- 조치: 작업 브랜치를 main에 머지하지 않고 수정
- 예상 복구 시간: 30분

---

## ✅ 완료 체크리스트

### Phase 1: 문서 및 계획
- [x] 작업 문서 작성
- [ ] 인프라팀과 Istio 헤더 이름 협의
- [ ] 작업 브랜치 생성: `feature/remove-spring-security`

### Phase 2: 파일 제거
- [ ] `SecurityConfig.kt` 삭제
- [ ] `JwtAuthenticationFilter.kt` 삭제
- [ ] `CustomAuthenticationEntryPoint.kt` 삭제
- [ ] `CustomAccessDeniedHandler.kt` 삭제
- [ ] `AuthenticationContext.kt` 삭제

### Phase 3: 코드 수정
- [ ] `JwtTokenProvider.kt` - 검증 로직 제거
- [ ] `CustomerAuthenticationController.kt` - logout 메서드 수정
- [ ] `OwnerAuthenticationController.kt` - logout 메서드 수정
- [ ] `IstioHeaderExtractor.kt` - 신규 생성 (옵션)
- [ ] 에러 핸들러 추가 (옵션)

### Phase 4: 의존성 및 설정
- [ ] `build.gradle.kts` - Spring Security 의존성 제거
- [ ] `application.yml` - 불필요한 설정 제거

### Phase 5: 테스트 수정
- [ ] 단위 테스트 수정 및 실행
- [ ] 통합 테스트 수정 및 실행
- [ ] 로컬 환경 테스트 (Mock 헤더)

### Phase 6: 배포 준비
- [ ] Dockerfile 빌드 확인
- [ ] Istio 설정 파일 준비
- [ ] NetworkPolicy 설정 준비
- [ ] 배포 후 테스트 시나리오 작성

### Phase 7: 배포 및 검증
- [ ] 개발 환경 배포
- [ ] API 동작 확인 (로그인, 로그아웃, 토큰 갱신)
- [ ] 모니터링 확인 (에러율, 응답 시간)
- [ ] 프로덕션 배포

---

## 📊 예상 영향 분석

### 긍정적 영향
- ✅ **코드 복잡도 감소**: 약 500줄의 Security 관련 코드 제거
- ✅ **책임 분리 명확화**: 인증/인가 = Istio, 토큰 발급 = Customer Service
- ✅ **유지보수성 향상**: JWT 검증 로직이 Istio 한 곳에만 존재
- ✅ **성능 향상**: 불필요한 Filter 제거로 응답 시간 단축 (예상: ~5ms)
- ✅ **테스트 단순화**: Security Mock 불필요

### 주의 필요 영향
- ⚠️ **Istio 의존성**: Istio 장애 시 인증/인가 불가
- ⚠️ **로컬 개발 복잡도**: Mock 헤더 주입 필요
- ⚠️ **배포 순서 중요**: Istio 설정 → Customer Service 순서로 배포

### 마이그레이션 영향
- 🔄 **API 클라이언트 영향**: 없음 (Endpoint 변경 없음)
- 🔄 **다른 서비스 영향**: 없음 (Customer Service만 수정)
- 🔄 **데이터베이스 영향**: 없음

---

## 📞 문의 및 지원

- **작업자**: @hayden-han
- **인프라 담당**: [인프라팀 담당자]
- **코드 리뷰어**: [리뷰어 명단]

---

## 📚 참고 자료

- [Istio RequestAuthentication 공식 문서](https://istio.io/latest/docs/reference/config/security/request_authentication/)
- [Istio AuthorizationPolicy 공식 문서](https://istio.io/latest/docs/reference/config/security/authorization-policy/)
- [JWT RFC 7519](https://datatracker.ietf.org/doc/html/rfc7519)
- [Spring Security 공식 문서](https://docs.spring.io/spring-security/reference/index.html)

---

**문서 버전**: 1.0
**최종 수정일**: 2025-11-13
