package com.groom.customer.application.service

import com.groom.customer.application.dto.RefreshTokenCommand
import com.groom.customer.domain.model.TokenCredentials
import com.groom.customer.domain.service.Authenticator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class RefreshTokenServiceTest :
    BehaviorSpec({
        val authenticator = mockk<Authenticator>()
        val refreshTokenService = RefreshTokenService(authenticator)

        Given("유효한 Refresh Token이 주어졌을 때") {
            val refreshTokenValue = "valid-refresh-token"
            val credentials =
                TokenCredentials(
                    primaryToken = "new-access-token",
                    secondaryToken = null,
                    validitySeconds = 300L,
                )

            every { authenticator.refreshCredentials(refreshTokenValue) } returns credentials

            When("토큰 갱신을 시도하면") {
                val command = RefreshTokenCommand(refreshToken = refreshTokenValue)
                val result = refreshTokenService.refresh(command)

                Then("새로운 Access Token이 반환되어야 한다") {
                    result.accessToken shouldBe "new-access-token"
                    result.expiresIn shouldBe 300L
                    result.tokenType shouldBe "Bearer"
                }

                Then("Authenticator의 refreshCredentials가 호출되어야 한다") {
                    verify { authenticator.refreshCredentials(refreshTokenValue) }
                }
            }
        }

        Given("유효하지 않은 Refresh Token이 주어졌을 때") {
            val invalidToken = "invalid-token"

            every { authenticator.refreshCredentials(invalidToken) } throws IllegalArgumentException("유효하지 않은 Refresh Token입니다.")

            When("토큰 갱신을 시도하면") {
                val command = RefreshTokenCommand(refreshToken = invalidToken)

                Then("예외가 발생해야 한다") {
                    shouldThrow<IllegalArgumentException> {
                        refreshTokenService.refresh(command)
                    }
                }
            }
        }
    })
