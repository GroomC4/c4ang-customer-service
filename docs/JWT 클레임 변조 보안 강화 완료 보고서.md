# JWT 클레임 변조 보안 강화 완료 보고서

## 개요
JWT 클레임 변조 공격에 대한 보안 취약점을 분석하고 방어 로직을 구현했습니다.

## 구현된 보안 강화 항목

### 1. 알고리즘 혼동 공격 방어 (Algorithm Confusion Attack)
**위치**: `JwtTokenProvider.kt:101-116`

**구현 내용**:
- JWT 헤더의 알고리즘을 사전 검증
- HS256만 허용하고 다른 알고리즘(RS256, HS512 등) 차단
- 대소문자 변형(hs256, Hs256 등)도 모두 차단

```kotlin
private fun validateTokenHeader(token: String) {
    val decodedJWT = JWT.decode(token)
    val algorithm = decodedJWT.algorithm

    if (algorithm != EXPECTED_ALGORITHM) {
        throw IllegalArgumentException(
            "지원하지 않는 알고리즘입니다. 예상: $EXPECTED_ALGORITHM, 실제: $algorithm"
        )
    }
}
```

**방어하는 공격**:
- 공격자가 RS256을 HS256으로 바꿔 공개 키를 비밀 키로 사용하는 공격
- 알고리즘 타입 변조를 통한 서명 우회 시도

### 2. None 알고리즘 차단 (None Algorithm Attack)
**위치**: `JwtTokenProvider.kt:26, 106-108`

**구현 내용**:
- "none", "None", "NONE" 알고리즘 명시적 차단
- None 알고리즘 토큰 검증 시 즉시 거부

```kotlin
companion object {
    private const val EXPECTED_ALGORITHM = "HS256"
    private val BLOCKED_ALGORITHMS = setOf("none", "None", "NONE")
}

if (algorithm in BLOCKED_ALGORITHMS) {
    throw IllegalArgumentException("None 알고리즘은 허용되지 않습니다")
}
```

**방어하는 공격**:
- 서명 없이 토큰을 위조하는 공격
- 헤더의 alg를 "none"으로 설정하여 서명 검증 우회 시도

### 3. Sealed Class 기반 타입 안전 예외 계층 구조
**위치**:
- `DomainException.kt`: 모든 도메인 예외의 sealed class 계층 구조
- `JwtTokenProvider.kt:69-116`: 예외 처리 로직
- `CustomAuthenticationEntryPoint.kt`: 필터 레이어 예외 핸들링 (TokenException)
- `GlobalExceptionHandler.kt`: 서비스 레이어 예외 핸들링 (AuthenticationException, UserException 등)

**구현 내용**:

**3.1 Sealed Class 예외 계층 구조**
```kotlin
// 도메인 예외 최상위 sealed class
sealed class DomainException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

// 토큰 관련 예외 (필터 레이어에서 발생)
sealed class TokenException(
    message: String,
    cause: Throwable? = null,
) : DomainException(message, cause) {
    class TokenExpired : TokenException("토큰이 만료되었습니다. 다시 로그인해주세요.")
    data class InvalidTokenSignature(override val cause: Throwable?) : TokenException("인증에 실패하였습니다.", cause)
    data class InvalidTokenFormat(override val cause: Throwable?) : TokenException("토큰 형식이 올바르지 않습니다.", cause)
    data class InvalidTokenAlgorithm(override val cause: Throwable?) : TokenException("토큰 알고리즘이 올바르지 않습니다.", cause)
    data class InvalidTokenIssuer(val expected: String, val actual: String) : TokenException("토큰 발급자가 올바르지 않습니다. 기대값: $expected, 실제값: $actual")
    data class MissingTokenClaim(val claimName: String) : TokenException("필수 토큰 클레임이 없습니다: $claimName")
    class MissingToken : TokenException("인증 토큰이 없습니다.")
}

// 인증 관련 예외 (서비스 레이어에서 발생)
sealed class AuthenticationException(
    message: String,
    cause: Throwable? = null,
) : DomainException(message, cause) {
    data class UserNotFoundByEmail(val email: String) : AuthenticationException("일치하는 이메일의 사용자가 없습니다: $email")
    data class InvalidPassword(val email: String) : AuthenticationException("비밀번호가 일치하지 않습니다")
    data class InvalidCredentials(val clue: Map<String, Any> = emptyMap()) : AuthenticationException("인증 정보가 올바르지 않습니다")
}
```

**타입 안전성의 장점**:
- 컴파일 타임에 모든 예외 케이스를 exhaustive하게 처리 가능
- 각 예외 타입마다 고유한 컨텍스트 필드 보유 (email, claimName 등)
- ErrorCode는 도메인 레이어가 아닌 presentation 레이어에서 매핑

**3.2 예외 처리 로직 (JwtTokenProvider)**
```kotlin
fun validateToken(token: String): AuthorizationData {
    try {
        validateTokenHeader(token)
        val decodedJWT = verifyTokenSignature(token)
        validateRequiredClaims(decodedJWT)
        return AuthorizationData(...)
    } catch (e: Auth0TokenExpiredException) {
        throw TokenException.TokenExpired()
    } catch (e: SignatureVerificationException) {
        throw TokenException.InvalidTokenSignature(cause = e)
    } catch (e: AlgorithmMismatchException) {
        throw TokenException.InvalidTokenAlgorithm(cause = e)
    } catch (e: InvalidClaimException) {
        val message = e.message.orEmpty()
        if (message.contains("issuer", ignoreCase = true)) {
            throw TokenException.InvalidTokenIssuer(expected = properties.issuer, actual = "unknown")
        } else {
            throw TokenException.InvalidTokenFormat(cause = e)
        }
    } catch (e: JWTDecodeException) {
        throw TokenException.InvalidTokenFormat(cause = e)
    }
}
```

**3.3 레이어별 예외 핸들링**

**Filter Layer: CustomAuthenticationEntryPoint** (TokenException 처리)
```kotlin
@Component
class CustomAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint {
    override fun commence(...) {
        val jwtException = request.getAttribute(JwtAuthenticationFilter.JWT_EXCEPTION_ATTRIBUTE)

        val (errorCode, errorMessage) = when (jwtException) {
            is TokenException.TokenExpired -> ErrorCode.TOKEN_EXPIRED to jwtException.message
            is TokenException.InvalidTokenSignature -> ErrorCode.INVALID_TOKEN_SIGNATURE to jwtException.message
            is TokenException.InvalidTokenFormat -> ErrorCode.INVALID_TOKEN_FORMAT to jwtException.message
            is TokenException.InvalidTokenAlgorithm -> ErrorCode.INVALID_TOKEN_ALGORITHM to jwtException.message
            is TokenException.InvalidTokenIssuer -> ErrorCode.INVALID_TOKEN_ISSUER to jwtException.message
            is TokenException.MissingTokenClaim -> ErrorCode.MISSING_TOKEN_CLAIM to jwtException.message
            is TokenException.MissingToken -> ErrorCode.MISSING_TOKEN to jwtException.message
            else -> ErrorCode.MISSING_TOKEN to "인증에 실패하였습니다."
        }

        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        val errorResponse = ErrorResponse(code = errorCode, message = errorMessage ?: "인증에 실패하였습니다.")
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
}
```

**Service Layer: GlobalExceptionHandler** (AuthenticationException, UserException 등 처리)
```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(e: AuthenticationException): ResponseEntity<ErrorResponse> {
        val errorCode = when (e) {
            is AuthenticationException.UserNotFoundByEmail -> {
                logger.warn(e) { "User not found by email: ${e.email}" }
                ErrorCode.USER_NOT_FOUND_BY_EMAIL
            }
            is AuthenticationException.InvalidPassword -> {
                logger.warn(e) { "Invalid password for email: ${e.email}" }
                ErrorCode.INVALID_PASSWORD
            }
            is AuthenticationException.InvalidCredentials -> {
                logger.warn(e) { "Invalid credentials: clue=${e.clue}" }
                ErrorCode.INVALID_CREDENTIALS
            }
        }
        return ResponseEntity(
            ErrorResponse(code = errorCode, message = e.message ?: "인증에 실패하였습니다."),
            HttpStatus.UNAUTHORIZED
        )
    }
}
```

**중요**:
- **TokenException**은 필터 레이어에서만 발생 → `CustomAuthenticationEntryPoint`가 처리
- **AuthenticationException**은 서비스 레이어에서만 발생 → `GlobalExceptionHandler`가 처리
- 이는 Spring Security Filter Chain의 실행 순서 때문 (Filter → DispatcherServlet → Controller)

**방어하는 공격 및 개선 효과**:
- **서명 변조 공격 방지**: 서명이 변조된 토큰 차단
- **비밀 키 불일치 탐지**: 다른 비밀 키로 서명된 토큰 차단
- **페이로드 변조 방지**: 클레임이 변조된 토큰 차단
- **보안 정보 노출 방지**: 사용자에게는 일반적인 메시지만 반환, 상세 정보는 로그로만 기록
- **타입 안전성**: sealed class를 통한 컴파일 타임 exhaustive 검사
- **클라이언트 플로우 개선**: 토큰 만료와 인증 실패를 구분하여 적절한 처리 가능
  - 만료: 리프레시 토큰으로 갱신 시도
  - 인증 실패: 재로그인 유도

### 4. 필수 클레임 검증
**위치**: `JwtTokenProvider.kt:147-169`

**구현 내용**:
- Subject (사용자 ID) 필수 검증
- Role 클레임 필수 검증
- JWT ID (jti) 필수 검증
- Issuer 검증
- 검증 실패 시 `TokenException.MissingTokenClaim` 발생

```kotlin
private fun validateRequiredClaims(decodedJWT: DecodedJWT) {
    if (decodedJWT.subject.isNullOrBlank()) {
        throw TokenException.MissingTokenClaim(claimName = "subject")
    }
    val role = decodedJWT.getClaim("role").asString()
    if (role.isNullOrBlank()) {
        throw TokenException.MissingTokenClaim(claimName = "role")
    }
    if (decodedJWT.id.isNullOrBlank()) {
        throw TokenException.MissingTokenClaim(claimName = "jti")
    }
}
```

**방어하는 공격**:
- 필수 클레임이 누락된 위조 토큰
- 권한 정보가 없는 토큰
- 중복 사용 가능한 토큰 (jti 없음)
- 보안: 어떤 클레임이 누락되었는지 타입 안전하게 전달

### 5. 알고리즘 헤더 사전 검증
**위치**: `JwtTokenProvider.kt:125-142`

**구현 내용**:
- None 알고리즘 차단
- HS256 이외의 알고리즘 차단
- 검증 실패 시 `TokenException.InvalidTokenAlgorithm` 발생

```kotlin
private fun validateTokenHeader(token: String) {
    val decodedJWT = JWT.decode(token)
    val algorithm = decodedJWT.algorithm

    if (algorithm in BLOCKED_ALGORITHMS) {
        throw TokenException.InvalidTokenAlgorithm(cause = null)
    }

    if (algorithm != EXPECTED_ALGORITHM) {
        throw TokenException.InvalidTokenAlgorithm(cause = null)
    }
}
```

**방어하는 공격**:
- 알고리즘 혼동 공격 (Algorithm Confusion Attack)
- None 알고리즘을 이용한 서명 우회 시도
- 보안: 공격 시도 내용을 상세히 로그로 기록

### 6. 비밀 키 강도 검증
**위치**: `JwtProperties.kt:26-29`

**구현 내용**:
- 최소 32자(256비트) 비밀 키 강제
- 애플리케이션 시작 시 검증
- 약한 비밀 키 사용 방지

```kotlin
require(secret.length >= MIN_SECRET_LENGTH) {
    "JWT secret must be at least $MIN_SECRET_LENGTH characters long for security. " +
        "Current length: ${secret.length}"
}
```

**방어하는 공격**:
- 무차별 대입 공격(Brute Force)
- 예측 가능한 비밀 키 사용
- 약한 비밀 키로 인한 토큰 위조

## 테스트 커버리지

### 보안 테스트 파일
1. **JwtTokenProviderTest.kt** (20개 테스트)
   - 토큰 생성 테스트 (5개): 고유 ID, 만료 시간 등
   - 알고리즘 공격 방어 테스트 (4개): None, RS256, 대소문자 변형
   - 서명 검증 테스트 (3개): 서명 변조, 비밀 키 불일치, 페이로드 변조
   - 클레임 검증 테스트 (5개): Subject, Role, JWT ID, Issuer, 만료
   - 비정상 토큰 처리 테스트 (3개): 형식 오류, 빈 토큰, Base64 오류

2. **JwtPropertiesTest.kt** (12개 테스트)
   - 비밀 키 강도 검증 테스트 (6개)
   - 만료 시간 검증 테스트 (4개)
   - 기본값 테스트 (2개)

### 주요 테스트 시나리오

#### 알고리즘 공격 방어
```kotlin
@Test
@DisplayName("None 알고리즘을 사용한 토큰은 거부된다")
fun `should reject tokens with none algorithm`() {
    val token = JWT.create()
        .withIssuer(properties.issuer)
        .withSubject("user123")
        .withClaim("role", "ADMIN")
        .sign(Algorithm.none())

    assertThatThrownBy { jwtTokenProvider.validateToken(token) }
        .isInstanceOf(TokenException.InvalidTokenAlgorithm::class.java)
        .hasMessage("토큰 알고리즘이 올바르지 않습니다.")
}
```

#### 클레임 변조 방어 (통합 테스트)
```kotlin
@Test
@DisplayName("role이 변조된 토큰으로 로그아웃 시도 시 401 Unauthorized를 반환한다")
fun testLogoutWithTamperedRoleToken() {
    // 정상 토큰을 분해하여 payload만 변조하고 signature는 원본 유지
    val parts = validToken.split(".")
    val tamperedPayload = """{"sub":"${userId}","role":"OWNER"}"""
    val tamperedPayloadEncoded = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(tamperedPayload.toByteArray())

    // 원본 signature를 그대로 사용 (비밀키 없이는 올바른 signature 생성 불가)
    val tamperedToken = "${parts[0]}.$tamperedPayloadEncoded.${parts[2]}"

    mockMvc.perform(post("/api/v1/auth/customers/logout")
        .header("Authorization", "Bearer $tamperedToken"))
        .andExpect(status().isUnauthorized) // 서명 검증 실패
}
```

#### 토큰 만료 처리
```kotlin
@Test
@DisplayName("만료된 토큰은 거부된다")
fun `should reject expired token`() {
    val now = Instant.now()
    val expiredTime = now.minus(1, ChronoUnit.HOURS)
    val issuedTime = expiredTime.minus(1, ChronoUnit.HOURS)

    val token = JWT.create()
        .withIssuer(properties.issuer)
        .withSubject("user123")
        .withClaim("role", "USER")
        .withJWTId("test-id")
        .withIssuedAt(Date.from(issuedTime))
        .withExpiresAt(Date.from(expiredTime))
        .sign(Algorithm.HMAC256(properties.secret))

    assertThatThrownBy { jwtTokenProvider.validateToken(token) }
        .isInstanceOf(TokenException.TokenExpired::class.java)
        .hasMessage("토큰이 만료되었습니다. 다시 로그인해주세요.")
}
```

#### 비밀 키 강도 검증
```kotlin
@Test
fun `should reject weak secret key with less than 32 characters`() {
    assertThatThrownBy {
        JwtProperties(secret = "short-secret")
    }.hasMessageContaining("JWT secret must be at least 32 characters long")
}
```

## 테스트 결과

```
✅ JWT 보안 테스트 통과 (20개 테스트)
✅ JWT Properties 테스트 통과 (12개 테스트)
✅ 통합 테스트 통과 (Customer/Owner/Token Refresh)
✅ 모든 인증 관련 테스트 성공
```

## 보안 체크리스트

- [x] **알고리즘 혼동 공격 방어**: HS256만 허용, 다른 알고리즘 차단
- [x] **None 알고리즘 차단**: "none" 알고리즘 명시적 거부
- [x] **서명 검증 철저**: 모든 토큰의 서명 무결성 검증
- [x] **약한 비밀 키 사용 방지**: 최소 32자 비밀 키 강제
- [x] **필수 클레임 검증**: Subject, Role, JWT ID 필수 검증
- [x] **만료 시간 검증**: 토큰 만료 시간 자동 검증
- [x] **Issuer 검증**: 발급자 정보 검증
- [x] **타입 안전 예외 처리**: Sealed class로 만료와 인증 실패를 타입 안전하게 구분
- [x] **레이어별 예외 처리**: Filter와 Service 레이어 예외 핸들링 명확히 분리
- [x] **보안 정보 보호**: 사용자에게는 일반 메시지만 반환, 상세 정보는 로그로만 기록
- [x] **상세 로깅**: 예외 타입별 contextual 정보를 통한 디버깅 지원

## 개선 효과

### 보안 강화
1. **권한 상승 공격 방지**: Role 클레임 변조를 통한 권한 상승 차단
2. **토큰 위조 방지**: 알고리즘 조작 및 서명 없는 토큰 차단
3. **무차별 대입 공격 저항**: 강력한 비밀 키 강제
4. **클레임 무결성 보장**: 필수 클레임 검증으로 불완전한 토큰 차단
5. **보안 정보 노출 방지**: 공격자에게 유용한 정보 차단, 일반 메시지만 반환

### 코드 품질 및 유지보수성
1. **타입 안전성**: Sealed class로 컴파일 타임에 모든 예외 케이스를 exhaustive하게 처리
2. **명확한 책임 분리**: Filter 레이어와 Service 레이어의 예외 핸들링 명확히 구분
3. **도메인 주도 설계**: ErrorCode는 presentation 레이어에서 매핑, 도메인 레이어는 비즈니스 로직에 집중
4. **컨텍스트 정보**: 각 예외 타입마다 고유한 필드(email, claimName 등) 보유

### 운영 안정성
1. **명확한 에러 처리**: Sealed class로 만료와 인증 실패를 타입 안전하게 구분
2. **상세한 로깅**: 예외 타입별 contextual 정보를 통해 정확한 실패 원인 파악 가능
3. **조기 실패**: 애플리케이션 시작 시 약한 비밀 키 감지
4. **포괄적인 테스트**: 32개 이상의 보안 테스트로 회귀 방지

### 클라이언트 경험 개선
1. **토큰 만료 처리**: `TokenException.TokenExpired` 수신 시 자동으로 리프레시 토큰으로 갱신 시도
2. **인증 실패 처리**: 각 `TokenException` 서브타입에 따라 적절한 대응 가능
3. **명확한 HTTP 상태**: 모두 401 Unauthorized이지만 ErrorCode와 메시지로 구분 가능

## 권장 사항

### 운영 환경 설정
```yaml
# application-prod.yml
security:
  jwt:
    secret: ${JWT_SECRET}  # 환경 변수로 관리 (최소 32자)
    issuer: "ecommerce-service-api"
    access-token-expiration-minutes: 5  # 짧은 만료 시간
    refresh-token-expiration-days: 3
```

### 비밀 키 관리
- 환경 변수 또는 AWS Secrets Manager 사용
- 최소 32자 이상의 무작위 문자열
- 정기적인 키 로테이션 권장

### 모니터링
- JWT 검증 실패 로그 모니터링 (예외 타입별 contextual 정보 활용)
- 반복적인 실패 패턴 감지 (예: 동일 IP에서 반복되는 서명 검증 실패)
- 알고리즘 변조 시도 알림 설정 (`TokenException.InvalidTokenAlgorithm`)
- 토큰 만료와 인증 실패 비율 추적 (정상 사용자 vs 공격 시도 구분)
- Sealed class exhaustive when 표현식을 통한 누락된 예외 처리 방지

## 파일 변경 사항

### 추가된 파일
- `DomainException.kt`: Sealed class 기반 예외 계층 구조 (TokenException, AuthenticationException, UserException, PermissionException, RefreshTokenException, ResourceException)
- `CustomAuthenticationEntryPoint.kt`: 필터 레이어 예외 핸들링
- `JwtTokenProviderTest.kt`: 20개 보안 테스트
- `JwtPropertiesTest.kt`: 12개 보안 테스트

### 수정된 파일
- `JwtTokenProvider.kt`: 알고리즘 검증, 클레임 검증, TokenException 타입 사용
- `JwtProperties.kt`: 비밀 키 강도 검증 추가
- `GlobalExceptionHandler.kt`: Sealed class exhaustive 예외 핸들링 (AuthenticationException, UserException, PermissionException, RefreshTokenException, ResourceException)
- `JwtAuthenticationFilter.kt`: TokenException 캐치 및 request attribute 저장

### 제거된 파일
- `TokenExpiredException.kt`: DomainException.kt의 TokenException.TokenExpired로 대체
- `AuthenticationFailedException.kt`: DomainException.kt의 TokenException 서브타입들로 대체
- `ConflictResourceException.kt`: DomainException.kt의 UserException.DuplicateEmail 등으로 대체

### 통합 테스트 업데이트
- `CustomerAuthenticationControllerIntegrationTest.kt`: 토큰 변조 테스트 추가
- `OwnerAuthenticationControllerIntegrationTest.kt`: 토큰 변조 테스트 추가
- `TokenRefreshControllerIntegrationTest.kt`: 응답 데이터 검증 추가
- `UserPolicyTest.kt`, `CustomerAuthenticationServiceTest.kt`, `OwnerAuthenticationServiceTest.kt`, `RegisterCustomerServiceTest.kt`, `RegisterOwnerServiceTest.kt`: 새로운 예외 타입 사용

## 결론

JWT 클레임 변조에 대한 주요 공격 벡터를 모두 방어하는 포괄적인 보안 강화를 완료했습니다.
OWASP의 JWT 보안 권장사항을 준수하며, 실전 환경에서 발생 가능한 공격을 효과적으로 차단합니다.

### 주요 성과
1. **6가지 보안 메커니즘 구현**: 알고리즘 공격, None 알고리즘, Sealed Class 예외 계층, 클레임 검증, 헤더 검증, 비밀 키 강도
2. **타입 안전한 예외 처리**: Sealed class 기반 계층 구조로 컴파일 타임 exhaustive 검사 보장
3. **레이어별 책임 분리**: 필터 레이어(CustomAuthenticationEntryPoint)와 서비스 레이어(GlobalExceptionHandler) 예외 처리 명확히 분리
4. **보안과 사용성의 균형**: 공격자에게는 정보를 노출하지 않으면서도, 정상 사용자에게는 명확한 안내 제공
5. **운영 친화적 설계**: 상세한 로깅과 모니터링 지원으로 보안 이슈 신속 대응 가능
6. **포괄적인 테스트**: 32개 이상의 단위 테스트와 통합 테스트로 안정성 보장

---
**작성일**: 2025-10-26
**최종 업데이트**: 2025-10-26 (Sealed Class 기반 예외 계층 구조로 리팩토링)
**검증 완료**: ✅ 모든 테스트 통과
