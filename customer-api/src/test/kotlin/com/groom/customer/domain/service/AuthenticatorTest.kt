package com.groom.customer.domain.service

import com.groom.customer.common.annotation.UnitTest
import com.groom.customer.common.exception.RefreshTokenException
import com.groom.customer.domain.model.RefreshToken
import com.groom.customer.domain.model.User
import com.groom.customer.domain.model.UserRole
import com.groom.customer.domain.port.GenerateTokenPort
import com.groom.customer.domain.port.LoadRefreshTokenPort
import com.groom.customer.domain.port.LoadUserPort
import com.groom.customer.domain.port.SaveRefreshTokenPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.UUID

@UnitTest
class AuthenticatorTest :
    BehaviorSpec({

        isolationMode = IsolationMode.InstancePerLeaf

        Given("유효한 사용자와 클라이언트 IP가 주어졌을 때") {
            val generateTokenPort = mockk<GenerateTokenPort>()
            val loadRefreshTokenPort = mockk<LoadRefreshTokenPort>()
            val saveRefreshTokenPort = mockk<SaveRefreshTokenPort>()
            val loadUserPort = mockk<LoadUserPort>()

            val authenticator =
                Authenticator(
                    generateTokenPort = generateTokenPort,
                    loadRefreshTokenPort = loadRefreshTokenPort,
                    saveRefreshTokenPort = saveRefreshTokenPort,
                    loadUserPort = loadUserPort,
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

            every { generateTokenPort.generateAccessToken(user) } returns accessToken
            every { generateTokenPort.generateRefreshToken(user) } returns refreshToken
            every { generateTokenPort.getAccessTokenValiditySeconds() } returns 300L
            every { generateTokenPort.getRefreshTokenValiditySeconds() } returns 3L
            every { loadRefreshTokenPort.loadByUserId(userId) } returns null
            every { saveRefreshTokenPort.save(any()) } answers { firstArg() }

            When("인증 자격증명을 생성하면") {
                val result = authenticator.createAndPersistCredentials(user, clientIp)

                Then("토큰이 생성되고 저장된다") {
                    result.primaryToken shouldBe accessToken
                    result.secondaryToken shouldBe refreshToken
                    result.getValiditySeconds() shouldBe 300L

                    verify(exactly = 1) { generateTokenPort.generateAccessToken(user) }
                    verify(exactly = 1) { generateTokenPort.generateRefreshToken(user) }
                    verify(exactly = 1) { saveRefreshTokenPort.save(any()) }
                }
            }
        }

        Given("이미 리프레시 토큰이 존재하는 사용자가 로그인할 때") {
            val generateTokenPort = mockk<GenerateTokenPort>()
            val loadRefreshTokenPort = mockk<LoadRefreshTokenPort>()
            val saveRefreshTokenPort = mockk<SaveRefreshTokenPort>()
            val loadUserPort = mockk<LoadUserPort>()

            val authenticator =
                Authenticator(
                    generateTokenPort = generateTokenPort,
                    loadRefreshTokenPort = loadRefreshTokenPort,
                    saveRefreshTokenPort = saveRefreshTokenPort,
                    loadUserPort = loadUserPort,
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

            every { generateTokenPort.generateAccessToken(user) } returns newAccessToken
            every { generateTokenPort.generateRefreshToken(user) } returns newRefreshToken
            every { generateTokenPort.getAccessTokenValiditySeconds() } returns 300L
            every { generateTokenPort.getRefreshTokenValiditySeconds() } returns 3L
            every { loadRefreshTokenPort.loadByUserId(userId) } returns existingRefreshToken
            every { existingRefreshToken.updateToken(any(), any()) } returns Unit
            every { saveRefreshTokenPort.save(any()) } answers { firstArg() }

            When("새로운 자격증명을 생성하면") {
                val result = authenticator.createAndPersistCredentials(user, clientIp)

                Then("기존 토큰이 갱신된다") {
                    result.primaryToken shouldBe newAccessToken
                    result.secondaryToken shouldBe newRefreshToken

                    verify(exactly = 1) { loadRefreshTokenPort.loadByUserId(userId) }
                    verify(exactly = 1) { existingRefreshToken.updateToken(any(), any()) }
                    verify(exactly = 1) { saveRefreshTokenPort.save(any()) }
                }
            }
        }

        Given("유효한 리프레시 토큰이 주어졌을 때") {
            val generateTokenPort = mockk<GenerateTokenPort>()
            val loadRefreshTokenPort = mockk<LoadRefreshTokenPort>()
            val saveRefreshTokenPort = mockk<SaveRefreshTokenPort>()
            val loadUserPort = mockk<LoadUserPort>()

            val authenticator =
                Authenticator(
                    generateTokenPort = generateTokenPort,
                    loadRefreshTokenPort = loadRefreshTokenPort,
                    saveRefreshTokenPort = saveRefreshTokenPort,
                    loadUserPort = loadUserPort,
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

            every { generateTokenPort.validateRefreshToken(refreshTokenValue) } returns Unit
            every { loadRefreshTokenPort.loadByToken(refreshTokenValue) } returns storedRefreshToken
            every { loadUserPort.loadById(userId) } returns user
            every { generateTokenPort.generateAccessToken(user) } returns newAccessToken
            every { generateTokenPort.getAccessTokenValiditySeconds() } returns 300L

            When("토큰을 갱신하면") {
                val result = authenticator.refreshCredentials(refreshTokenValue)

                Then("새로운 액세스 토큰이 발급된다") {
                    result.primaryToken shouldBe newAccessToken
                    result.secondaryToken shouldBe null
                    result.getValiditySeconds() shouldBe 300L

                    verify(exactly = 1) { generateTokenPort.validateRefreshToken(refreshTokenValue) }
                    verify(exactly = 1) { loadRefreshTokenPort.loadByToken(refreshTokenValue) }
                    verify(exactly = 1) { loadUserPort.loadById(userId) }
                    verify(exactly = 1) { generateTokenPort.generateAccessToken(user) }
                }
            }
        }

        Given("만료된 리프레시 토큰이 주어졌을 때") {
            val generateTokenPort = mockk<GenerateTokenPort>()
            val loadRefreshTokenPort = mockk<LoadRefreshTokenPort>()
            val saveRefreshTokenPort = mockk<SaveRefreshTokenPort>()
            val loadUserPort = mockk<LoadUserPort>()

            val authenticator =
                Authenticator(
                    generateTokenPort = generateTokenPort,
                    loadRefreshTokenPort = loadRefreshTokenPort,
                    saveRefreshTokenPort = saveRefreshTokenPort,
                    loadUserPort = loadUserPort,
                )

            val refreshTokenValue = "expired-refresh-token"

            val expiredRefreshToken =
                mockk<RefreshToken>(relaxed = true) {
                    every { userId } returns UUID.randomUUID()
                    every { token } returns refreshTokenValue
                    every { expiresAt } returns LocalDateTime.now().minusDays(1) // 만료됨
                    every { isExpired(any()) } returns true
                }

            every { generateTokenPort.validateRefreshToken(refreshTokenValue) } returns Unit
            every { loadRefreshTokenPort.loadByToken(refreshTokenValue) } returns expiredRefreshToken

            When("토큰을 갱신하려고 하면") {
                Then("예외가 발생한다") {
                    val exception =
                        shouldThrow<RefreshTokenException.RefreshTokenExpired> {
                            authenticator.refreshCredentials(refreshTokenValue)
                        }
                    exception.message shouldBe "리프레시 토큰이 만료되었습니다"

                    verify(exactly = 1) { loadRefreshTokenPort.loadByToken(refreshTokenValue) }
                }
            }
        }

        Given("사용자가 로그아웃할 때") {
            val generateTokenPort = mockk<GenerateTokenPort>()
            val loadRefreshTokenPort = mockk<LoadRefreshTokenPort>()
            val saveRefreshTokenPort = mockk<SaveRefreshTokenPort>()
            val loadUserPort = mockk<LoadUserPort>()

            val authenticator =
                Authenticator(
                    generateTokenPort = generateTokenPort,
                    loadRefreshTokenPort = loadRefreshTokenPort,
                    saveRefreshTokenPort = saveRefreshTokenPort,
                    loadUserPort = loadUserPort,
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

            every { loadRefreshTokenPort.loadByUserId(userId) } returns refreshToken
            every { saveRefreshTokenPort.save(any()) } answers { firstArg() }

            When("자격증명을 취소하면") {
                authenticator.revokeCredentials(user)

                Then("리프레시 토큰이 무효화된다") {
                    verify(exactly = 1) { loadRefreshTokenPort.loadByUserId(userId) }
                    verify(exactly = 1) { refreshToken.invalidate() }
                    verify(exactly = 1) { saveRefreshTokenPort.save(refreshToken) }
                }
            }
        }

        Given("리프레시 토큰이 없는 사용자가 로그아웃할 때") {
            val generateTokenPort = mockk<GenerateTokenPort>()
            val loadRefreshTokenPort = mockk<LoadRefreshTokenPort>()
            val saveRefreshTokenPort = mockk<SaveRefreshTokenPort>()
            val loadUserPort = mockk<LoadUserPort>()

            val authenticator =
                Authenticator(
                    generateTokenPort = generateTokenPort,
                    loadRefreshTokenPort = loadRefreshTokenPort,
                    saveRefreshTokenPort = saveRefreshTokenPort,
                    loadUserPort = loadUserPort,
                )

            val userId = UUID.randomUUID()
            val user =
                mockk<User>(relaxed = true) {
                    every { id } returns userId
                }

            every { loadRefreshTokenPort.loadByUserId(userId) } returns null

            When("자격증명을 취소하려고 하면") {
                Then("예외가 발생한다") {
                    val exception =
                        shouldThrow<IllegalArgumentException> {
                            authenticator.revokeCredentials(user)
                        }
                    exception.message shouldBe "로그아웃할 수 없습니다. 유효한 세션이 존재하지 않습니다."

                    verify(exactly = 1) { loadRefreshTokenPort.loadByUserId(userId) }
                    verify(exactly = 0) { saveRefreshTokenPort.save(any()) }
                }
            }
        }
    })
