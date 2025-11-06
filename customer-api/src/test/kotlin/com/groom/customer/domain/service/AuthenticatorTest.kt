package com.groom.customer.domain.service

import com.groom.customer.common.annotation.UnitTest
import com.groom.customer.common.enums.UserRole
import com.groom.customer.common.exception.RefreshTokenException
import com.groom.customer.domain.model.RefreshToken
import com.groom.customer.domain.model.User
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

@UnitTest
class AuthenticatorTest :
    BehaviorSpec({

        isolationMode = IsolationMode.InstancePerLeaf

        Given("유효한 사용자와 클라이언트 IP가 주어졌을 때") {
            val tokenProvider = mockk<TokenProvider>()
            val refreshTokenStore = mockk<RefreshTokenStore>()
            val userReader = mockk<UserReader>()

            val authenticator =
                Authenticator(
                    tokenProvider = tokenProvider,
                    refreshTokenStore = refreshTokenStore,
                    userReader = userReader,
                )

            val userId = UUID.randomUUID()
            val user =
                mockk<User>(relaxed = true) {
                    every { id } returns userId
                    every { email } returns "user@example.com"
                    every { role } returns UserRole.CUSTOMER
                }

            val clientIp = "192.168.1.1"
            val accessToken = "access-token-12345"
            val refreshToken = "refresh-token-67890"

            every { tokenProvider.generateAccessToken(user) } returns accessToken
            every { tokenProvider.generateRefreshToken(user) } returns refreshToken
            every { tokenProvider.getAccessTokenValiditySeconds() } returns 300L
            every { tokenProvider.getRefreshTokenValiditySeconds() } returns 3L
            every { refreshTokenStore.findByUserId(userId) } returns Optional.empty()
            every { refreshTokenStore.save(any()) } answers { firstArg() }

            When("인증 자격증명을 생성하면") {
                val result = authenticator.createAndPersistCredentials(user, clientIp)

                Then("토큰이 생성되고 저장된다") {
                    result.primaryToken shouldBe accessToken
                    result.secondaryToken shouldBe refreshToken
                    result.getValiditySeconds() shouldBe 300L

                    verify(exactly = 1) { tokenProvider.generateAccessToken(user) }
                    verify(exactly = 1) { tokenProvider.generateRefreshToken(user) }
                    verify(exactly = 1) { refreshTokenStore.save(any()) }
                }
            }
        }

        Given("이미 리프레시 토큰이 존재하는 사용자가 로그인할 때") {
            val tokenProvider = mockk<TokenProvider>()
            val refreshTokenStore = mockk<RefreshTokenStore>()
            val userReader = mockk<UserReader>()

            val authenticator =
                Authenticator(
                    tokenProvider = tokenProvider,
                    refreshTokenStore = refreshTokenStore,
                    userReader = userReader,
                )

            val userId = UUID.randomUUID()
            val user =
                mockk<User>(relaxed = true) {
                    every { id } returns userId
                    every { email } returns "user@example.com"
                    every { role } returns UserRole.CUSTOMER
                }

            val existingRefreshToken =
                mockk<RefreshToken>(relaxed = true)

            val clientIp = "192.168.1.1"
            val newAccessToken = "new-access-token"
            val newRefreshToken = "new-refresh-token"

            every { tokenProvider.generateAccessToken(user) } returns newAccessToken
            every { tokenProvider.generateRefreshToken(user) } returns newRefreshToken
            every { tokenProvider.getAccessTokenValiditySeconds() } returns 300L
            every { tokenProvider.getRefreshTokenValiditySeconds() } returns 3L
            every { refreshTokenStore.findByUserId(userId) } returns Optional.of(existingRefreshToken)
            every { existingRefreshToken.updateToken(any(), any()) } returns Unit
            every { refreshTokenStore.save(any()) } answers { firstArg() }

            When("새로운 자격증명을 생성하면") {
                val result = authenticator.createAndPersistCredentials(user, clientIp)

                Then("기존 토큰이 갱신된다") {
                    result.primaryToken shouldBe newAccessToken
                    result.secondaryToken shouldBe newRefreshToken

                    verify(exactly = 1) { refreshTokenStore.findByUserId(userId) }
                    verify(exactly = 1) { existingRefreshToken.updateToken(any(), any()) }
                    verify(exactly = 1) { refreshTokenStore.save(any()) }
                }
            }
        }

        Given("유효한 리프레시 토큰이 주어졌을 때") {
            val tokenProvider = mockk<TokenProvider>()
            val refreshTokenStore = mockk<RefreshTokenStore>()
            val userReader = mockk<UserReader>()

            val authenticator =
                Authenticator(
                    tokenProvider = tokenProvider,
                    refreshTokenStore = refreshTokenStore,
                    userReader = userReader,
                )

            val userId = UUID.randomUUID()
            val refreshTokenValue = "valid-refresh-token"

            val storedRefreshToken =
                mockk<RefreshToken>(relaxed = true) {
                    every { this@mockk.userId } returns userId
                    every { token } returns refreshTokenValue
                    every { expiresAt } returns LocalDateTime.now().plusDays(1)
                    every { isExpired(any()) } returns false
                }

            val user =
                mockk<User>(relaxed = true) {
                    every { id } returns userId
                    every { email } returns "user@example.com"
                    every { role } returns UserRole.CUSTOMER
                }

            val newAccessToken = "new-access-token"

            every { tokenProvider.validateRefreshToken(refreshTokenValue) } returns Unit
            every { refreshTokenStore.findByToken(refreshTokenValue) } returns Optional.of(storedRefreshToken)
            every { userReader.findById(userId) } returns Optional.of(user)
            every { tokenProvider.generateAccessToken(user) } returns newAccessToken
            every { tokenProvider.getAccessTokenValiditySeconds() } returns 300L

            When("토큰을 갱신하면") {
                val result = authenticator.refreshCredentials(refreshTokenValue)

                Then("새로운 액세스 토큰이 발급된다") {
                    result.primaryToken shouldBe newAccessToken
                    result.secondaryToken shouldBe null
                    result.getValiditySeconds() shouldBe 300L

                    verify(exactly = 1) { tokenProvider.validateRefreshToken(refreshTokenValue) }
                    verify(exactly = 1) { refreshTokenStore.findByToken(refreshTokenValue) }
                    verify(exactly = 1) { userReader.findById(userId) }
                    verify(exactly = 1) { tokenProvider.generateAccessToken(user) }
                }
            }
        }

        Given("만료된 리프레시 토큰이 주어졌을 때") {
            val tokenProvider = mockk<TokenProvider>()
            val refreshTokenStore = mockk<RefreshTokenStore>()
            val userReader = mockk<UserReader>()

            val authenticator =
                Authenticator(
                    tokenProvider = tokenProvider,
                    refreshTokenStore = refreshTokenStore,
                    userReader = userReader,
                )

            val refreshTokenValue = "expired-refresh-token"

            val expiredRefreshToken =
                mockk<RefreshToken>(relaxed = true) {
                    every { userId } returns UUID.randomUUID()
                    every { token } returns refreshTokenValue
                    every { expiresAt } returns LocalDateTime.now().minusDays(1) // 만료됨
                    every { isExpired(any()) } returns true
                }

            every { tokenProvider.validateRefreshToken(refreshTokenValue) } returns Unit
            every { refreshTokenStore.findByToken(refreshTokenValue) } returns Optional.of(expiredRefreshToken)

            When("토큰을 갱신하려고 하면") {
                Then("예외가 발생한다") {
                    val exception =
                        shouldThrow<RefreshTokenException.RefreshTokenExpired> {
                            authenticator.refreshCredentials(refreshTokenValue)
                        }
                    exception.message shouldBe "리프레시 토큰이 만료되었습니다"

                    verify(exactly = 1) { refreshTokenStore.findByToken(refreshTokenValue) }
                }
            }
        }

        Given("사용자가 로그아웃할 때") {
            val tokenProvider = mockk<TokenProvider>()
            val refreshTokenStore = mockk<RefreshTokenStore>()
            val userReader = mockk<UserReader>()

            val authenticator =
                Authenticator(
                    tokenProvider = tokenProvider,
                    refreshTokenStore = refreshTokenStore,
                    userReader = userReader,
                )

            val userId = UUID.randomUUID()
            val user =
                mockk<User>(relaxed = true) {
                    every { id } returns userId
                }

            val refreshToken =
                mockk<RefreshToken>(relaxed = true) {
                    every { this@mockk.userId } returns userId
                }

            every { refreshTokenStore.findByUserId(userId) } returns Optional.of(refreshToken)
            every { refreshTokenStore.save(any()) } answers { firstArg() }

            When("자격증명을 취소하면") {
                authenticator.revokeCredentials(user)

                Then("리프레시 토큰이 무효화된다") {
                    verify(exactly = 1) { refreshTokenStore.findByUserId(userId) }
                    verify(exactly = 1) { refreshToken.invalidate() }
                    verify(exactly = 1) { refreshTokenStore.save(refreshToken) }
                }
            }
        }

        Given("리프레시 토큰이 없는 사용자가 로그아웃할 때") {
            val tokenProvider = mockk<TokenProvider>()
            val refreshTokenStore = mockk<RefreshTokenStore>()
            val userReader = mockk<UserReader>()

            val authenticator =
                Authenticator(
                    tokenProvider = tokenProvider,
                    refreshTokenStore = refreshTokenStore,
                    userReader = userReader,
                )

            val userId = UUID.randomUUID()
            val user =
                mockk<User>(relaxed = true) {
                    every { id } returns userId
                }

            every { refreshTokenStore.findByUserId(userId) } returns Optional.empty()

            When("자격증명을 취소하려고 하면") {
                Then("예외가 발생한다") {
                    val exception =
                        shouldThrow<IllegalArgumentException> {
                            authenticator.revokeCredentials(user)
                        }
                    exception.message shouldBe "로그아웃할 수 없습니다. 유효한 세션이 존재하지 않습니다."

                    verify(exactly = 1) { refreshTokenStore.findByUserId(userId) }
                    verify(exactly = 0) { refreshTokenStore.save(any()) }
                }
            }
        }
    })
