package com.groom.customer.domain.model

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime
import java.util.UUID

class RefreshTokenTest :
    ShouldSpec({
        context("RefreshToken 생성") {
            should("모든 필수 속성을 가진 RefreshToken을 생성할 수 있다") {
                // given
                val userId = UUID.randomUUID()
                val token = "sample-refresh-token"
                val clientIp = "192.168.1.1"
                val expiresAt = LocalDateTime.now().plusDays(3)

                // when
                val refreshToken =
                    RefreshToken(
                        userId = userId,
                        token = token,
                        clientIp = clientIp,
                        expiresAt = expiresAt,
                    )

                // then
                refreshToken.userId shouldBe userId
                refreshToken.token shouldBe token
                refreshToken.clientIp shouldBe clientIp
                refreshToken.expiresAt shouldBe expiresAt
            }

            should("clientIp가 null이어도 RefreshToken을 생성할 수 있다") {
                // given
                val userId = UUID.randomUUID()
                val token = "sample-refresh-token"
                val expiresAt = LocalDateTime.now().plusDays(3)

                // when
                val refreshToken =
                    RefreshToken(
                        userId = userId,
                        token = token,
                        clientIp = null,
                        expiresAt = expiresAt,
                    )

                // then
                refreshToken.clientIp shouldBe null
            }
        }

        context("updateToken()") {
            should("토큰과 만료 시간을 새로운 값으로 갱신할 수 있다") {
                // given
                val refreshToken =
                    RefreshToken(
                        userId = UUID.randomUUID(),
                        token = "old-token",
                        clientIp = "127.0.0.1",
                        expiresAt = LocalDateTime.now().plusDays(1),
                    )

                val newToken = "new-token"
                val newExpiresAt = LocalDateTime.now().plusDays(3)

                // when
                refreshToken.updateToken(newToken, newExpiresAt)

                // then
                refreshToken.token shouldBe newToken
                refreshToken.expiresAt shouldBe newExpiresAt
            }

            should("기존 토큰이 null이어도 새로운 토큰으로 갱신할 수 있다") {
                // given
                val refreshToken =
                    RefreshToken(
                        userId = UUID.randomUUID(),
                        token = null,
                        clientIp = "127.0.0.1",
                        expiresAt = LocalDateTime.now().plusDays(1),
                    )

                val newToken = "new-token"
                val newExpiresAt = LocalDateTime.now().plusDays(3)

                // when
                refreshToken.updateToken(newToken, newExpiresAt)

                // then
                refreshToken.token shouldBe newToken
                refreshToken.expiresAt shouldBe newExpiresAt
            }
        }

        context("invalidate()") {
            should("토큰을 null로 설정하여 무효화할 수 있다") {
                // given
                val refreshToken =
                    RefreshToken(
                        userId = UUID.randomUUID(),
                        token = "valid-token",
                        clientIp = "127.0.0.1",
                        expiresAt = LocalDateTime.now().plusDays(3),
                    )

                // when
                refreshToken.invalidate()

                // then
                refreshToken.token shouldBe null
            }

            should("이미 무효화된 토큰을 다시 무효화해도 문제없다") {
                // given
                val refreshToken =
                    RefreshToken(
                        userId = UUID.randomUUID(),
                        token = null,
                        clientIp = "127.0.0.1",
                        expiresAt = LocalDateTime.now().plusDays(3),
                    )

                // when
                refreshToken.invalidate()

                // then
                refreshToken.token shouldBe null
            }
        }

        context("isExpired()") {
            should("현재 시간이 만료 시간 이후라면 true를 반환한다") {
                // given
                val pastTime = LocalDateTime.now().minusDays(1)
                val refreshToken =
                    RefreshToken(
                        userId = UUID.randomUUID(),
                        token = "token",
                        clientIp = "127.0.0.1",
                        expiresAt = pastTime,
                    )

                // when
                val now = LocalDateTime.now()
                val result = refreshToken.isExpired(now)

                // then
                result shouldBe true
            }

            should("현재 시간이 만료 시간 이전이라면 false를 반환한다") {
                // given
                val futureTime = LocalDateTime.now().plusDays(1)
                val refreshToken =
                    RefreshToken(
                        userId = UUID.randomUUID(),
                        token = "token",
                        clientIp = "127.0.0.1",
                        expiresAt = futureTime,
                    )

                // when
                val now = LocalDateTime.now()
                val result = refreshToken.isExpired(now)

                // then
                result shouldBe false
            }

            should("현재 시간과 만료 시간이 정확히 같다면 false를 반환한다") {
                // given
                val exactTime = LocalDateTime.now()
                val refreshToken =
                    RefreshToken(
                        userId = UUID.randomUUID(),
                        token = "token",
                        clientIp = "127.0.0.1",
                        expiresAt = exactTime,
                    )

                // when
                val result = refreshToken.isExpired(exactTime)

                // then
                result shouldBe false
            }
        }

        context("isValid()") {
            should("토큰이 존재하고 만료되지 않았다면 true를 반환한다") {
                // given
                val futureTime = LocalDateTime.now().plusDays(1)
                val refreshToken =
                    RefreshToken(
                        userId = UUID.randomUUID(),
                        token = "valid-token",
                        clientIp = "127.0.0.1",
                        expiresAt = futureTime,
                    )

                // when
                val now = LocalDateTime.now()
                val result = refreshToken.isValid(now)

                // then
                result shouldBe true
            }

            should("토큰이 null이라면 false를 반환한다") {
                // given
                val futureTime = LocalDateTime.now().plusDays(1)
                val refreshToken =
                    RefreshToken(
                        userId = UUID.randomUUID(),
                        token = null,
                        clientIp = "127.0.0.1",
                        expiresAt = futureTime,
                    )

                // when
                val now = LocalDateTime.now()
                val result = refreshToken.isValid(now)

                // then
                result shouldBe false
            }

            should("토큰이 존재하지만 만료되었다면 false를 반환한다") {
                // given
                val pastTime = LocalDateTime.now().minusDays(1)
                val refreshToken =
                    RefreshToken(
                        userId = UUID.randomUUID(),
                        token = "expired-token",
                        clientIp = "127.0.0.1",
                        expiresAt = pastTime,
                    )

                // when
                val now = LocalDateTime.now()
                val result = refreshToken.isValid(now)

                // then
                result shouldBe false
            }

            should("토큰이 null이고 만료되었다면 false를 반환한다") {
                // given
                val pastTime = LocalDateTime.now().minusDays(1)
                val refreshToken =
                    RefreshToken(
                        userId = UUID.randomUUID(),
                        token = null,
                        clientIp = "127.0.0.1",
                        expiresAt = pastTime,
                    )

                // when
                val now = LocalDateTime.now()
                val result = refreshToken.isValid(now)

                // then
                result shouldBe false
            }
        }

        context("도메인 시나리오 테스트") {
            should("로그인 -> 재로그인 -> 로그아웃 시나리오를 올바르게 처리한다") {
                // given - 최초 로그인
                val userId = UUID.randomUUID()
                val refreshToken =
                    RefreshToken(
                        userId = userId,
                        token = "first-login-token",
                        clientIp = "192.168.1.1",
                        expiresAt = LocalDateTime.now().plusDays(3),
                    )

                // then - 최초 로그인 상태 확인
                refreshToken.isValid(LocalDateTime.now()) shouldBe true

                // when - 재로그인 (토큰 갱신)
                val newToken = "second-login-token"
                val newExpiresAt = LocalDateTime.now().plusDays(3)
                refreshToken.updateToken(newToken, newExpiresAt)

                // then - 갱신된 토큰 확인
                refreshToken.token shouldBe newToken
                refreshToken.isValid(LocalDateTime.now()) shouldBe true

                // when - 로그아웃 (토큰 무효화)
                refreshToken.invalidate()

                // then - 무효화된 상태 확인
                refreshToken.token shouldBe null
                refreshToken.isValid(LocalDateTime.now()) shouldBe false
            }

            should("만료된 토큰은 무효화하지 않아도 유효하지 않다") {
                // given - 만료된 토큰
                val pastTime = LocalDateTime.now().minusDays(1)
                val refreshToken =
                    RefreshToken(
                        userId = UUID.randomUUID(),
                        token = "expired-token",
                        clientIp = "127.0.0.1",
                        expiresAt = pastTime,
                    )

                // then
                refreshToken.token shouldBe "expired-token" // 토큰은 존재하지만
                refreshToken.isValid(LocalDateTime.now()) shouldBe false // 만료되어 유효하지 않음
            }
        }
    })
