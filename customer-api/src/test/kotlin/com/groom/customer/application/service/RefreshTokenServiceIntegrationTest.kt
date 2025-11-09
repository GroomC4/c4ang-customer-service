package com.groom.customer.application.service

import com.groom.customer.application.dto.LoginCommand
import com.groom.customer.application.dto.RefreshTokenCommand
import com.groom.customer.application.dto.RegisterCustomerCommand
import com.groom.customer.common.AbstractIntegrationTest
import com.groom.customer.common.TransactionApplier
import com.groom.customer.common.exception.RefreshTokenException
import com.groom.customer.outbound.repository.RefreshTokenRepositoryImpl
import com.groom.customer.outbound.repository.UserRepositoryImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Refresh Token 서비스 통합 테스트")
class RefreshTokenServiceIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var refreshTokenService: RefreshTokenService

    @Autowired
    private lateinit var customerAuthenticationService: CustomerAuthenticationService

    @Autowired
    private lateinit var registerCustomerService: RegisterCustomerService

    @Autowired
    private lateinit var userRepository: UserRepositoryImpl

    @Autowired
    private lateinit var refreshTokenRepository: RefreshTokenRepositoryImpl

    @Autowired
    private lateinit var transactionApplier: TransactionApplier

    private val createdEmails = mutableListOf<String>()

    @BeforeEach
    fun setUp() {
        createdEmails.clear()
    }

    @AfterEach
    fun tearDown() {
        // 테스트 데이터 정리 - Primary 트랜잭션으로 실행
        transactionApplier.applyPrimaryTransaction {
            createdEmails.forEach { email ->
                userRepository.findByEmail(email).ifPresent { user ->
                    refreshTokenRepository.findByUserId(user.id).ifPresent { token ->
                        refreshTokenRepository.delete(token)
                    }
                    userRepository.delete(user)
                }
            }
        }
    }

    private fun trackEmail(email: String) {
        createdEmails.add(email)
    }

    @Nested
    @DisplayName("정상 케이스")
    inner class SuccessCases {
        @Test
        @DisplayName("유효한 Refresh Token으로 요청 시 새로운 Access Token을 발급받는다")
        fun testSuccessfulRefresh() {
            // given - Primary 트랜잭션으로 사용자 등록 및 로그인
            val registerCommand =
                RegisterCustomerCommand(
                    username = "토큰갱신테스트",
                    email = "refresh@example.com",
                    rawPassword = "password123!",
                    defaultAddress = "서울시 강남구",
                    defaultPhoneNumber = "010-1111-2222",
                )
            trackEmail(registerCommand.email)

            transactionApplier.applyPrimaryTransaction {
                registerCustomerService.register(registerCommand)
            }

            val loginCommand =
                LoginCommand(
                    email = "refresh@example.com",
                    password = "password123!",
                    clientIp = "127.0.0.1",
                )

            val loginResult =
                transactionApplier.applyPrimaryTransaction {
                    customerAuthenticationService.login(loginCommand)
                }

            // when - Primary 트랜잭션으로 토큰 갱신
            val refreshCommand = RefreshTokenCommand(refreshToken = loginResult.refreshToken)
            val result =
                transactionApplier.applyPrimaryTransaction {
                    refreshTokenService.refresh(refreshCommand)
                }

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
                    transactionApplier.applyPrimaryTransaction {
                        refreshTokenService.refresh(refreshCommand)
                    }
                }
            assertThat(exception.message).isEqualTo("리프레시 토큰을 찾을 수 없습니다")
        }

        @Test
        @DisplayName("무효화된 Refresh Token으로 요청 시 예외가 발생한다")
        fun testRefreshWithInvalidatedToken() {
            // given - Primary 트랜잭션으로 사용자 등록, 로그인, 로그아웃
            val registerCommand =
                RegisterCustomerCommand(
                    username = "무효화토큰",
                    email = "invalidated@example.com",
                    rawPassword = "password123!",
                    defaultAddress = "서울시 서초구",
                    defaultPhoneNumber = "010-3333-4444",
                )
            trackEmail(registerCommand.email)

            transactionApplier.applyPrimaryTransaction {
                registerCustomerService.register(registerCommand)
            }

            val loginCommand =
                LoginCommand(
                    email = "invalidated@example.com",
                    password = "password123!",
                    clientIp = "127.0.0.1",
                )

            val loginResult =
                transactionApplier.applyPrimaryTransaction {
                    customerAuthenticationService.login(loginCommand)
                }

            // 로그아웃하여 토큰 무효화
            val userId =
                transactionApplier.applyPrimaryTransaction {
                    userRepository.findByEmail("invalidated@example.com").get().id!!
                }

            transactionApplier.applyPrimaryTransaction {
                customerAuthenticationService.logout(
                    com.groom.customer.application.dto
                        .LogoutCommand(userId = userId),
                )
            }

            // when & then - 무효화된 토큰으로 갱신 시도
            val refreshCommand = RefreshTokenCommand(refreshToken = loginResult.refreshToken)
            val exception =
                assertThrows<RefreshTokenException.RefreshTokenNotFound> {
                    transactionApplier.applyPrimaryTransaction {
                        refreshTokenService.refresh(refreshCommand)
                    }
                }
            assertThat(exception.message).isEqualTo("리프레시 토큰을 찾을 수 없습니다")
        }
    }
}
