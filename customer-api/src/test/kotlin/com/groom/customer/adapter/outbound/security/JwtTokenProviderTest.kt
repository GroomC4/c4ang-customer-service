package com.groom.customer.adapter.outbound.security

import com.auth0.jwt.JWT
import com.groom.customer.adapter.outbound.security.jwt.AuthorizationData
import com.groom.customer.adapter.outbound.security.jwt.JwtTokenProvider
import com.groom.customer.configuration.jwt.JwtProperties
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

    companion object {
        // 테스트용 RSA 키 (한 줄로 작성)
        private const val TEST_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----\n" +
            "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCvD5SDEkoxLqiL" +
            "gTeNbjLe2vpkgDEGtGh1lYPB/AALcEEC23v6EhMiTx41DDIw9X6oD+G/7GwbKpa9" +
            "sxhvjYtGBUuMPmUhXwR9No13mRXeeiacbnsFRMUzp9iP9z0/vJTCGINcRrZA56B6" +
            "buCbDq/d0H4YdE+Pz7+cMIsMGBrpg3YO2m1XDQaDWuxfXLv0WfVmvReAv+gyRT7K" +
            "NzzvMdGeqD7eiMWYOkRqfvNY+Sox5EdlTB4/JTOWVO1JV8f2JcFWcb8vxH3i/+cZ" +
            "H11tOi7TWdM5Glz242nf3pEWKfjytzf1+/OgkXFOm9a8i8UigCS9jzeRo0xFYK32" +
            "+KYvhfHRAgMBAAECggEAQ1cg79KHS6gBGbjZH8R2ORfPHf3Z3hRj4mdjSamgcsX5" +
            "nBnF9Qoi5h29JvbMD90/nXKOin9tjn2xgsNz8OVn38WFrCsMR+v/FBN6E7mFmhEu" +
            "7RnqpLoxiY9VVPvsSapHJuq7DTH+RbVUHASuzba2nALpnoqPWGi38mMR+dMD9zMB" +
            "t8N0KEz4CvA8bfOKrh1xGskFPNRf2ScOJ2JmqkoBGyXrAobJGwp27Au7UNEdR496" +
            "uRglbOrurr/eK9A1LvulVShgpxHX4FVPWJK3Zjdgc7tkqhBUpQh8uv0hlEL8Sv+1" +
            "aluV5u958x9G86ioNAiblBoZnYRRcPB1Y37txwmWgQKBgQDZakjqZEFtuORxHFmK" +
            "6LK2L9SdgCu/st85Ms0b35bI7MOMeYr2U5M4+e3hkxL3C9tFpZqtQIGYYU2l0OnC" +
            "oLw1ctESZgGM/KiAaUydKvamszT1JtT2vtLGlpnYg7RjV7+sIaQUkH283n8BO+Ro" +
            "vjqJG6XZbGrczgJ/01gHDM01CQKBgQDOIQhnT1BBHbqBDEyaiTmf7PCkBiCM2eiJ" +
            "fx5vfSXQcktRkze8XKRs1qKVJiQfR3TtmJa1fMwl3NbkkR9FawNYqSO2kAROr9Gl" +
            "i6wAPooPO5Vl2GES5V+5nI8G/iy1aPcYXq5Dz/yf1o+kbO/ytwERUXglTaMHnI4w" +
            "NhCtDEQQiQKBgGKfzR3OhsOgKLiKtK/HqTHt9pPPzYizOoF24wYu4faZOIejpv7g" +
            "oJsq/Nbj4amBjmFEoyrOZTtbgF6kqzWntljEkcS30yJChqlhmuh80dCC4JYInHil" +
            "zXVaYcWO0ShzaLZLuGO/u9oOUCyeH5nIGUOS8CP2A2/QX9/eXkMscnYJAoGAEqRM" +
            "JUO4B1uP7XHWT7ePXZZJIRxovzRJ4n17nCueSt67TxJYXRGn0SwMIh8D70xAF+jP" +
            "4HP75oS1bpBtWpLWB6OsVitqKE+gTy91i8QcKkqCNWa/SL0zzg6JpOFB29o1Vp/h" +
            "dMKPn0kBTqaHgNTqJM3QZtdBokOXXGbXVT8hvLkCgYARs07jQ+hdwz4kz2P3X++C" +
            "CkD3HEvJYbx5smrwvYu6DQgOvOdl4wFYKmmd35/S2u7Two8xKZysEZs9Ts6FxPLG" +
            "H59lezuu8Uh/uKx34qGueJie9iXkwtcadD1H3lSpyck+hvzJkbNH4peDWhtthW+P" +
            "eTiMG2mOPQRt+kY6QDPz8w==\n" +
            "-----END PRIVATE KEY-----"

        private const val TEST_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----\n" +
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArw+UgxJKMS6oi4E3jW4y" +
            "3tr6ZIAxBrRodZWDwfwAC3BBAtt7+hITIk8eNQwyMPV+qA/hv+xsGyqWvbMYb42L" +
            "RgVLjD5lIV8EfTaNd5kV3nomnG57BUTFM6fYj/c9P7yUwhiDXEa2QOegem7gmw6v" +
            "3dB+GHRPj8+/nDCLDBga6YN2DtptVw0Gg1rsX1y79Fn1Zr0XgL/oMkU+yjc87zHR" +
            "nqg+3ojFmDpEan7zWPkqMeRHZUwePyUzllTtSVfH9iXBVnG/L8R94v/nGR9dbTou" +
            "01nTORpc9uNp396RFin48rc39fvzoJFxTpvWvIvFIoAkvY83kaNMRWCt9vimL4Xx" +
            "0QIDAQAB\n" +
            "-----END PUBLIC KEY-----"
    }

    @BeforeEach
    fun setUp() {
        properties = JwtProperties(
            privateKey = TEST_PRIVATE_KEY,
            publicKey = TEST_PUBLIC_KEY,
            keyId = "test-key-1",
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
    @DisplayName("생성된 토큰은 RS256 알고리즘을 사용한다")
    fun `should use RS256 algorithm for token generation`() {
        // given
        val authData = AuthorizationData(id = "user789", roleName = "USER")

        // when
        val token = jwtTokenProvider.generateAccessToken(authData)
        val decodedJWT = JWT.decode(token)

        // then
        assertThat(decodedJWT.algorithm).isEqualTo("RS256")
    }

    @Test
    @DisplayName("생성된 토큰은 key id를 포함한다")
    fun `should include key id in generated token`() {
        // given
        val authData = AuthorizationData(id = "user123", roleName = "USER")

        // when
        val token = jwtTokenProvider.generateAccessToken(authData)
        val decodedJWT = JWT.decode(token)

        // then
        assertThat(decodedJWT.keyId).isEqualTo("test-key-1")
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

    @Test
    @DisplayName("공개키가 올바르게 노출된다")
    fun `should expose public key correctly`() {
        // then
        assertThat(jwtTokenProvider.publicKey).isNotNull
        assertThat(jwtTokenProvider.keyId).isEqualTo("test-key-1")
    }
}
