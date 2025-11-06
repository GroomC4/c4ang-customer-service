package com.groom.customer.security.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.groom.customer.common.exception.TokenException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

@DisplayName("JwtTokenProvider 보안 테스트")
class JwtTokenProviderTest {
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var properties: JwtProperties

    // 현재 시스템 시간을 사용
    // 참고: mockkStatic(System::class)으로 시간을 모킹하려 하면 JVM 전체에 영향을 주어
    // 테스트가 hang되거나 예측 불가능한 동작을 할 수 있습니다.
    // 실용적인 접근: 토큰 생성/검증 모두 현재 시간 사용, 테스트는 상대적 시간으로 검증
    private val clock = Clock.systemUTC()

    @BeforeEach
    fun setUp() {
        properties =
            JwtProperties(
                secret = "this-is-a-very-strong-secret-key-with-more-than-32-characters",
                issuer = "test-issuer",
                accessTokenExpirationMinutes = 30,
                refreshTokenExpirationDays = 7,
            )
        jwtTokenProvider = JwtTokenProvider(properties, clock)
    }

    @Nested
    @DisplayName("토큰 생성 테스트")
    inner class TokenGenerationTest {
        @Test
        @DisplayName("액세스 토큰을 정상적으로 생성한다")
        fun `should generate access token successfully`() {
            // given
            val authData = AuthorizationData(id = "user123", roleName = "USER")

            // when
            val token = jwtTokenProvider.generateAccessToken(authData)

            // then
            assertThat(token).isNotBlank()
            val validatedData = jwtTokenProvider.validateToken(token)
            assertThat(validatedData.id).isEqualTo("user123")
            assertThat(validatedData.roleName).isEqualTo("USER")
        }

        @Test
        @DisplayName("리프레시 토큰을 정상적으로 생성한다")
        fun `should generate refresh token successfully`() {
            // given
            val authData = AuthorizationData(id = "user456", roleName = "ADMIN")

            // when
            val token = jwtTokenProvider.generateRefreshToken(authData)

            // then
            assertThat(token).isNotBlank()
            val validatedData = jwtTokenProvider.validateToken(token)
            assertThat(validatedData.id).isEqualTo("user456")
            assertThat(validatedData.roleName).isEqualTo("ADMIN")
        }

        @Test
        @DisplayName("생성된 토큰은 HS256 알고리즘을 사용한다")
        fun `should use HS256 algorithm for token generation`() {
            // given
            val authData = AuthorizationData(id = "user789", roleName = "USER")

            // when
            val token = jwtTokenProvider.generateAccessToken(authData)
            val decodedJWT = JWT.decode(token)

            // then
            assertThat(decodedJWT.algorithm).isEqualTo("HS256")
        }

        @Test
        @DisplayName("생성된 토큰은 고유 ID(jti)를 포함한다")
        fun `should include unique JWT ID in generated token`() {
            // given
            val authData = AuthorizationData(id = "user123", roleName = "USER")

            // when
            val token1 = jwtTokenProvider.generateAccessToken(authData)
            val token2 = jwtTokenProvider.generateAccessToken(authData)
            val decodedJWT1 = JWT.decode(token1)
            val decodedJWT2 = JWT.decode(token2)

            // then
            assertThat(decodedJWT1.id).isNotBlank()
            assertThat(decodedJWT2.id).isNotBlank()
            assertThat(decodedJWT1.id).isNotEqualTo(decodedJWT2.id) // 매번 다른 ID
        }

        @Test
        @DisplayName("토큰의 만료 시간이 올바르게 설정된다")
        fun `should set correct expiration time for token`() {
            // given
            val authData = AuthorizationData(id = "user123", roleName = "USER")

            // when
            val beforeGeneration = Instant.now()
            val token = jwtTokenProvider.generateAccessToken(authData)
            val afterGeneration = Instant.now()
            val decodedJWT = JWT.decode(token)

            // then: 토큰의 만료 시간이 30분 후로 설정됨 (±1초 오차 허용)
            val expectedExpiresAt = beforeGeneration.plus(30, ChronoUnit.MINUTES)
            val actualExpiresAt = decodedJWT.expiresAt.toInstant()

            assertThat(actualExpiresAt).isBetween(
                expectedExpiresAt.minusSeconds(1),
                afterGeneration.plus(30, ChronoUnit.MINUTES).plusSeconds(1),
            )
        }
    }

    @Nested
    @DisplayName("알고리즘 공격 방어 테스트")
    inner class AlgorithmAttackDefenseTest {
        @Test
        @DisplayName("None 알고리즘을 사용한 토큰은 거부된다")
        fun `should reject tokens with none algorithm`() {
            // given: None 알고리즘으로 서명된 토큰 생성
            val token =
                JWT
                    .create()
                    .withIssuer(properties.issuer)
                    .withSubject("user123")
                    .withClaim("role", "ADMIN") // 권한 상승 시도
                    .sign(Algorithm.none())

            // when & then
            assertThatThrownBy { jwtTokenProvider.validateToken(token) }
                .isInstanceOf(TokenException.InvalidTokenAlgorithm::class.java)
                .hasMessage("토큰 알고리즘이 올바르지 않습니다.")
        }

        @Test
        @DisplayName("알고리즘 혼동 공격을 차단한다 - HS256 대신 RS256 사용")
        fun `should reject algorithm confusion attack - RS256 instead of HS256`() {
            // given: 다른 알고리즘(RS256)으로 서명된 토큰
            // 실제로는 키 쌍 생성이 필요하지만, 여기서는 헤더만 조작
            val fakeToken = createTokenWithFakeAlgorithm("RS256")

            // when & then
            assertThatThrownBy { jwtTokenProvider.validateToken(fakeToken) }
                .isInstanceOf(TokenException.InvalidTokenAlgorithm::class.java)
                .hasMessage("토큰 알고리즘이 올바르지 않습니다.")
        }

        @Test
        @DisplayName("알고리즘 혼동 공격을 차단한다 - 대소문자 변형")
        fun `should reject algorithm confusion attack - case variations`() {
            // given: 대소문자를 변형한 알고리즘
            val testCases = listOf("hs256", "Hs256", "hS256")

            testCases.forEach { algorithm ->
                val fakeToken = createTokenWithFakeAlgorithm(algorithm)

                // when & then
                assertThatThrownBy { jwtTokenProvider.validateToken(fakeToken) }
                    .isInstanceOf(TokenException.InvalidTokenAlgorithm::class.java)
                    .hasMessage("토큰 알고리즘이 올바르지 않습니다.")
            }
        }

        private fun createTokenWithFakeAlgorithm(algorithm: String): String {
            // 헤더를 수동으로 조작하여 다른 알고리즘을 사용하는 것처럼 보이게 함
            val header = """{"alg":"$algorithm","typ":"JWT"}"""
            val payload = """{"sub":"user123","role":"ADMIN","iss":"${properties.issuer}"}"""
            val headerEncoded =
                java.util.Base64
                    .getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(header.toByteArray())
            val payloadEncoded =
                java.util.Base64
                    .getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(payload.toByteArray())
            return "$headerEncoded.$payloadEncoded.fake-signature"
        }
    }

    @Nested
    @DisplayName("서명 검증 테스트")
    inner class SignatureVerificationTest {
        @Test
        @DisplayName("잘못된 서명을 가진 토큰은 거부된다")
        fun `should reject token with invalid signature`() {
            // given: 정상 토큰을 생성한 후 서명 부분을 변조
            val authData = AuthorizationData(id = "user123", roleName = "USER")
            val validToken = jwtTokenProvider.generateAccessToken(authData)
            val parts = validToken.split(".")
            val tamperedToken = "${parts[0]}.${parts[1]}.invalid-signature"

            // when & then
            assertThatThrownBy { jwtTokenProvider.validateToken(tamperedToken) }
                .isInstanceOf(TokenException.InvalidTokenSignature::class.java)
                .hasMessage("인증에 실패하였습니다.")
        }

        @Test
        @DisplayName("다른 비밀 키로 서명된 토큰은 거부된다")
        fun `should reject token signed with different secret key`() {
            // given: 다른 비밀 키를 사용하는 Provider
            val differentProperties =
                JwtProperties(
                    secret = "this-is-a-different-secret-key-with-more-than-32-characters",
                    issuer = properties.issuer,
                )
            val differentProvider = JwtTokenProvider(differentProperties, clock)
            val authData = AuthorizationData(id = "user123", roleName = "USER")
            val tokenFromDifferentProvider = differentProvider.generateAccessToken(authData)

            // when & then: 원본 Provider로 검증하면 실패
            assertThatThrownBy { jwtTokenProvider.validateToken(tokenFromDifferentProvider) }
                .isInstanceOf(TokenException.InvalidTokenSignature::class.java)
                .hasMessage("인증에 실패하였습니다.")
        }

        @Test
        @DisplayName("페이로드가 변조된 토큰은 거부된다")
        fun `should reject token with tampered payload`() {
            // given: 정상 토큰 생성
            val authData = AuthorizationData(id = "user123", roleName = "USER")
            val validToken = jwtTokenProvider.generateAccessToken(authData)
            val parts = validToken.split(".")

            // 페이로드 변조 (role을 ADMIN으로 변경)
            val tamperedPayload = """{"sub":"user123","role":"ADMIN","iss":"${properties.issuer}"}"""
            val tamperedPayloadEncoded =
                java.util.Base64
                    .getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(tamperedPayload.toByteArray())
            val tamperedToken = "${parts[0]}.$tamperedPayloadEncoded.${parts[2]}"

            // when & then
            assertThatThrownBy { jwtTokenProvider.validateToken(tamperedToken) }
                .isInstanceOf(TokenException.InvalidTokenSignature::class.java)
                .hasMessage("인증에 실패하였습니다.")
        }
    }

    @Nested
    @DisplayName("클레임 검증 테스트")
    inner class ClaimValidationTest {
        @Test
        @DisplayName("Subject(사용자 ID)가 없는 토큰은 거부된다")
        fun `should reject token without subject`() {
            // given: Subject 없이 토큰 생성
            val token =
                JWT
                    .create()
                    .withIssuer(properties.issuer)
                    .withClaim("role", "USER")
                    .withJWTId("test-id")
                    .sign(Algorithm.HMAC256(properties.secret))

            // when & then
            assertThatThrownBy { jwtTokenProvider.validateToken(token) }
                .isInstanceOf(TokenException.MissingTokenClaim::class.java)
                .hasMessage("필수 토큰 클레임이 없습니다: subject")
        }

        @Test
        @DisplayName("Role 클레임이 없는 토큰은 거부된다")
        fun `should reject token without role claim`() {
            // given: Role 없이 토큰 생성
            val token =
                JWT
                    .create()
                    .withIssuer(properties.issuer)
                    .withSubject("user123")
                    .withJWTId("test-id")
                    .sign(Algorithm.HMAC256(properties.secret))

            // when & then
            assertThatThrownBy { jwtTokenProvider.validateToken(token) }
                .isInstanceOf(TokenException.MissingTokenClaim::class.java)
                .hasMessage("필수 토큰 클레임이 없습니다: role")
        }

        @Test
        @DisplayName("JWT ID가 없는 토큰은 거부된다")
        fun `should reject token without JWT ID`() {
            // given: JWT ID 없이 토큰 생성
            val token =
                JWT
                    .create()
                    .withIssuer(properties.issuer)
                    .withSubject("user123")
                    .withClaim("role", "USER")
                    .sign(Algorithm.HMAC256(properties.secret))

            // when & then
            assertThatThrownBy { jwtTokenProvider.validateToken(token) }
                .isInstanceOf(TokenException.MissingTokenClaim::class.java)
                .hasMessage("필수 토큰 클레임이 없습니다: jti")
        }

        @Test
        @DisplayName("잘못된 Issuer를 가진 토큰은 거부된다")
        fun `should reject token with invalid issuer`() {
            // given: 다른 Issuer로 토큰 생성
            val token =
                JWT
                    .create()
                    .withIssuer("malicious-issuer")
                    .withSubject("user123")
                    .withClaim("role", "ADMIN")
                    .withJWTId("test-id")
                    .sign(Algorithm.HMAC256(properties.secret))

            // when & then
            assertThatThrownBy { jwtTokenProvider.validateToken(token) }
                .isInstanceOf(TokenException.InvalidTokenIssuer::class.java)
        }

        @Test
        @DisplayName("만료된 토큰은 거부된다")
        fun `should reject expired token`() {
            // given: 현재 시간 기준 1시간 전에 만료된 토큰 생성
            val now = Instant.now()
            val expiredTime = now.minus(1, ChronoUnit.HOURS)
            val issuedTime = expiredTime.minus(1, ChronoUnit.HOURS)

            val token =
                JWT
                    .create()
                    .withIssuer(properties.issuer)
                    .withSubject("user123")
                    .withClaim("role", "USER")
                    .withJWTId("test-id")
                    .withIssuedAt(Date.from(issuedTime))
                    .withExpiresAt(Date.from(expiredTime))
                    .sign(Algorithm.HMAC256(properties.secret))

            // when & then
            assertThatThrownBy { jwtTokenProvider.validateToken(token) }
                .isInstanceOf(TokenException.TokenExpired::class.java)
                .hasMessage("토큰이 만료되었습니다. 다시 로그인해주세요.")
        }
    }

    @Nested
    @DisplayName("비정상 토큰 처리 테스트")
    inner class MalformedTokenTest {
        @Test
        @DisplayName("형식이 잘못된 토큰은 거부된다")
        fun `should reject malformed token`() {
            // given
            val malformedToken = "this.is.not.a.valid.jwt.token"

            // when & then
            assertThatThrownBy { jwtTokenProvider.validateToken(malformedToken) }
                .isInstanceOf(TokenException.InvalidTokenFormat::class.java)
                .hasMessage("토큰 형식이 올바르지 않습니다.")
        }

        @Test
        @DisplayName("빈 토큰은 거부된다")
        fun `should reject empty token`() {
            // given
            val emptyToken = ""

            // when & then
            assertThatThrownBy { jwtTokenProvider.validateToken(emptyToken) }
                .isInstanceOf(TokenException.InvalidTokenFormat::class.java)
        }

        @Test
        @DisplayName("Base64 디코딩이 불가능한 토큰은 거부된다")
        fun `should reject token with invalid base64 encoding`() {
            // given
            val invalidBase64Token = "invalid@base64.invalid@base64.invalid@base64"

            // when & then
            assertThatThrownBy { jwtTokenProvider.validateToken(invalidBase64Token) }
                .isInstanceOf(TokenException.InvalidTokenFormat::class.java)
        }
    }
}
