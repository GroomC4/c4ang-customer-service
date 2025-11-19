package com.groom.customer.application.service

import com.auth0.jwt.JWT
import com.groom.customer.application.dto.LoginCommand
import com.groom.customer.application.dto.LogoutCommand
import com.groom.customer.application.dto.RefreshTokenCommand
import com.groom.customer.common.exception.RefreshTokenException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlGroup
import java.util.UUID

@DisplayName("Refresh Token 서비스 통합 테스트")
@SpringBootTest(properties = ["spring.profiles.active=test"])
@SqlGroup(
    Sql(scripts = ["/sql/integration/refresh-token-service-test-data.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
    Sql(scripts = ["/sql/integration/cleanup.sql"], executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD),
)
class RefreshTokenServiceIntegrationTest {
    @Autowired
    private lateinit var refreshTokenService: RefreshTokenService

    @Autowired
    private lateinit var customerAuthenticationService: CustomerAuthenticationService

    @Nested
    @DisplayName("정상 케이스")
    inner class SuccessCases {
        @Test
        @DisplayName("유효한 Refresh Token으로 요청 시 새로운 Access Token을 발급받는다")
        fun testSuccessfulRefresh() {
            // given - SQL로 사용자 데이터 준비됨
            val loginCommand =
                LoginCommand(
                    email = "refresh@example.com",
                    password = "password123!",
                    clientIp = "127.0.0.1",
                )
            val loginResult = customerAuthenticationService.login(loginCommand)

            // when - 토큰 갱신
            val refreshCommand = RefreshTokenCommand(refreshToken = loginResult.refreshToken)
            val result = refreshTokenService.refresh(refreshCommand)

            // then
            assertThat(result.accessToken).isNotNull.isNotEmpty
            assertThat(result.accessToken).isNotEqualTo(loginResult.accessToken) // 새로운 access token
            assertThat(result.expiresIn).isEqualTo(300L)
            assertThat(result.tokenType).isEqualTo("Bearer")
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    inner class FailureCases {
        @Test
        @DisplayName("존재하지 않는 Refresh Token으로 요청 시 예외가 발생한다")
        fun testRefreshWithNonExistentToken() {
            // given
            val invalidToken = "non-existent-token"
            val refreshCommand = RefreshTokenCommand(refreshToken = invalidToken)

            // when & then
            val exception =
                assertThrows<RefreshTokenException.RefreshTokenNotFound> {
                    refreshTokenService.refresh(refreshCommand)
                }
            assertThat(exception.message).isEqualTo("리프레시 토큰을 찾을 수 없습니다")
        }

        @Test
        @DisplayName("무효화된 Refresh Token으로 요청 시 예외가 발생한다")
        fun testRefreshWithInvalidatedToken() {
            // given - SQL로 사용자 데이터 준비됨, 로그인 후 로그아웃으로 토큰 무효화
            val loginCommand =
                LoginCommand(
                    email = "invalidated@example.com",
                    password = "password123!",
                    clientIp = "127.0.0.1",
                )
            val loginResult = customerAuthenticationService.login(loginCommand)

            // JWT에서 userId 추출
            val decodedJWT = JWT.decode(loginResult.accessToken)
            val userId = UUID.fromString(decodedJWT.subject)

            // 로그아웃하여 토큰 무효화
            val logoutCommand = LogoutCommand(userId = userId)
            customerAuthenticationService.logout(logoutCommand)

            // when & then - 무효화된 토큰으로 갱신 시도
            val refreshCommand = RefreshTokenCommand(refreshToken = loginResult.refreshToken)
            val exception =
                assertThrows<RefreshTokenException.RefreshTokenNotFound> {
                    refreshTokenService.refresh(refreshCommand)
                }
            assertThat(exception.message).isEqualTo("리프레시 토큰을 찾을 수 없습니다")
        }
    }
}
