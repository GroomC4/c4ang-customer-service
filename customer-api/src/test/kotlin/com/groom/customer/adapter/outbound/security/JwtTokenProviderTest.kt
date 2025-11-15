package com.groom.customer.adapter.outbound.security

import com.auth0.jwt.JWT
import com.groom.customer.security.jwt.AuthorizationData
import com.groom.customer.security.jwt.JwtProperties
import com.groom.customer.security.jwt.JwtTokenProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit

@DisplayName("JwtTokenProvider 토큰 발급 테스트")
class JwtTokenProviderTest {
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var properties: JwtProperties
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

    @Test
    @DisplayName("액세스 토큰을 정상적으로 생성한다")
    fun `should generate access token successfully`() {
        // given
        val authData = AuthorizationData(id = "user123", roleName = "USER")

        // when
        val token = jwtTokenProvider.generateAccessToken(authData)

        // then
        assertThat(token).isNotBlank()
        val decodedJWT = JWT.decode(token)
        assertThat(decodedJWT.subject).isEqualTo("user123")
        assertThat(decodedJWT.getClaim("role").asString()).isEqualTo("USER")
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
        val decodedJWT = JWT.decode(token)
        assertThat(decodedJWT.subject).isEqualTo("user456")
        assertThat(decodedJWT.getClaim("role").asString()).isEqualTo("ADMIN")
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
    @DisplayName("Access Token의 만료 시간이 올바르게 설정된다")
    fun `should set correct expiration time for access token`() {
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

    @Test
    @DisplayName("Refresh Token의 만료 시간이 올바르게 설정된다")
    fun `should set correct expiration time for refresh token`() {
        // given
        val authData = AuthorizationData(id = "user123", roleName = "USER")

        // when
        val beforeGeneration = Instant.now()
        val token = jwtTokenProvider.generateRefreshToken(authData)
        val afterGeneration = Instant.now()
        val decodedJWT = JWT.decode(token)

        // then: 토큰의 만료 시간이 7일 후로 설정됨 (±1초 오차 허용)
        val expectedExpiresAt = beforeGeneration.plus(7, ChronoUnit.DAYS)
        val actualExpiresAt = decodedJWT.expiresAt.toInstant()

        assertThat(actualExpiresAt).isBetween(
            expectedExpiresAt.minusSeconds(1),
            afterGeneration.plus(7, ChronoUnit.DAYS).plusSeconds(1),
        )
    }

    @Test
    @DisplayName("토큰에 issuer가 올바르게 설정된다")
    fun `should set correct issuer in token`() {
        // given
        val authData = AuthorizationData(id = "user123", roleName = "USER")

        // when
        val token = jwtTokenProvider.generateAccessToken(authData)
        val decodedJWT = JWT.decode(token)

        // then
        assertThat(decodedJWT.issuer).isEqualTo("test-issuer")
    }
}
