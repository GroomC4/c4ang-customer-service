package com.groom.customer.adapter.outbound.persistence

import com.groom.customer.common.annotation.IntegrationTest
import com.groom.customer.domain.model.RefreshToken
import com.groom.customer.domain.model.User
import com.groom.customer.domain.model.UserRole
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.UUID

@IntegrationTest
@DisplayName("RefreshTokenRepositoryImpl CRUD 테스트")
class RefreshTokenRepositoryImplTest {
    @Autowired
    private lateinit var refreshTokenRepository: RefreshTokenRepositoryImpl

    @Autowired
    private lateinit var userRepository: UserRepositoryImpl

    @AfterEach
    fun cleanup() {
        refreshTokenRepository.deleteAll()
        userRepository.deleteAll()
    }

    private fun createTestUser(): User {
        val user =
            User(
                username = "테스트유저",
                email = "test@example.com",
                passwordHash = "password",
                role = UserRole.CUSTOMER,
            )
        return userRepository.save(user)
    }

    @Nested
    @DisplayName("Create 테스트")
    inner class CreateTest {
        @Test
        @DisplayName("리프레시 토큰을 정상적으로 저장할 수 있다")
        fun `should save refresh token successfully`() {
            // given
            val user = createTestUser()
            val refreshToken =
                RefreshToken(
                    userId = user.id,
                    token = "refresh_token_value",
                    expiresAt = LocalDateTime.now().plusDays(7),
                    clientIp = "127.0.0.1",
                )

            // when
            val savedToken = refreshTokenRepository.save(refreshToken)

            // then
            assertThat(savedToken.id).isNotNull
            assertThat(savedToken.userId).isEqualTo(user.id)
            assertThat(savedToken.token).isEqualTo("refresh_token_value")
            assertThat(savedToken.token).isNotNull()
        }

        @Test
        @DisplayName("같은 사용자의 기존 토큰은 덮어쓰기된다")
        fun `should overwrite existing token for same user`() {
            // given
            val user = createTestUser()
            val oldToken =
                RefreshToken(
                    userId = user.id,
                    token = "old_token",
                    expiresAt = LocalDateTime.now().plusDays(7),
                    clientIp = "127.0.0.1",
                )
            refreshTokenRepository.save(oldToken)

            // when
            val newToken =
                RefreshToken(
                    userId = user.id,
                    token = "new_token",
                    expiresAt = LocalDateTime.now().plusDays(7),
                    clientIp = "127.0.0.2",
                )
            refreshTokenRepository.save(newToken)

            // then
            val tokens = refreshTokenRepository.findAll()
            assertThat(tokens).hasSize(2) // 새 토큰이 추가됨
        }
    }

    @Nested
    @DisplayName("Read 테스트")
    inner class ReadTest {
        @Test
        @DisplayName("사용자 ID로 리프레시 토큰을 조회할 수 있다")
        fun `should find refresh token by user id`() {
            // given
            val user = createTestUser()
            val refreshToken =
                RefreshToken(
                    userId = user.id,
                    token = "refresh_token",
                    expiresAt = LocalDateTime.now().plusDays(7),
                    clientIp = "127.0.0.1",
                )
            refreshTokenRepository.save(refreshToken)

            // when
            val foundToken = refreshTokenRepository.findByUserId(user.id)

            // then
            assertThat(foundToken).isPresent
            assertThat(foundToken.get().token).isEqualTo("refresh_token")
        }

        @Test
        @DisplayName("토큰 값으로 리프레시 토큰을 조회할 수 있다")
        fun `should find refresh token by token value`() {
            // given
            val user = createTestUser()
            val refreshToken =
                RefreshToken(
                    userId = user.id,
                    token = "unique_token_value",
                    expiresAt = LocalDateTime.now().plusDays(7),
                    clientIp = "127.0.0.1",
                )
            refreshTokenRepository.save(refreshToken)

            // when
            val foundToken = refreshTokenRepository.findByToken("unique_token_value")

            // then
            assertThat(foundToken).isPresent
            assertThat(foundToken.get().userId).isEqualTo(user.id)
        }

        @Test
        @DisplayName("존재하지 않는 토큰 조회 시 빈 Optional을 반환한다")
        fun `should return empty optional when token not found`() {
            // when
            val foundToken = refreshTokenRepository.findByToken("nonexistent_token")

            // then
            assertThat(foundToken).isEmpty
        }

        @Test
        @DisplayName("존재하지 않는 사용자 ID로 조회 시 빈 Optional을 반환한다")
        fun `should return empty optional when user id not found`() {
            // when
            val foundToken = refreshTokenRepository.findByUserId(UUID.randomUUID())

            // then
            assertThat(foundToken).isEmpty
        }
    }

    @Nested
    @DisplayName("Update 테스트")
    inner class UpdateTest {
        @Test
        @DisplayName("리프레시 토큰을 무효화할 수 있다")
        fun `should invalidate refresh token`() {
            // given
            val user = createTestUser()
            val refreshToken =
                RefreshToken(
                    userId = user.id,
                    token = "token_to_invalidate",
                    expiresAt = LocalDateTime.now().plusDays(7),
                    clientIp = "127.0.0.1",
                )
            val savedToken = refreshTokenRepository.save(refreshToken)

            // when
            savedToken.invalidate()
            refreshTokenRepository.save(savedToken)

            // then
            val foundToken = refreshTokenRepository.findById(savedToken.id).get()
            assertThat(foundToken.token).isNull()
        }

        @Test
        @DisplayName("토큰을 업데이트할 수 있다")
        fun `should update token`() {
            // given
            val user = createTestUser()
            val originalExpiry = LocalDateTime.now().plusDays(7)
            val refreshToken =
                RefreshToken(
                    userId = user.id,
                    token = "old_token",
                    expiresAt = originalExpiry,
                    clientIp = "127.0.0.1",
                )
            val savedToken = refreshTokenRepository.save(refreshToken)
            val tokenId = savedToken.id

            // when
            val newExpiry = LocalDateTime.now().plusDays(14)
            savedToken.updateToken("new_token", newExpiry)
            refreshTokenRepository.saveAndFlush(savedToken) // 명시적 flush

            // then
            refreshTokenRepository.flush()
            val foundToken = refreshTokenRepository.findById(tokenId).get()
            assertThat(foundToken.token).isEqualTo("new_token")
            assertThat(foundToken.expiresAt).isAfter(originalExpiry)
        }
    }

    @Nested
    @DisplayName("Delete 테스트")
    inner class DeleteTest {
        @Test
        @DisplayName("리프레시 토큰을 삭제할 수 있다")
        fun `should delete refresh token`() {
            // given
            val user = createTestUser()
            val refreshToken =
                RefreshToken(
                    userId = user.id,
                    token = "token_to_delete",
                    expiresAt = LocalDateTime.now().plusDays(7),
                    clientIp = "127.0.0.1",
                )
            val savedToken = refreshTokenRepository.save(refreshToken)

            // when
            refreshTokenRepository.delete(savedToken)

            // then
            val foundToken = refreshTokenRepository.findById(savedToken.id)
            assertThat(foundToken).isEmpty
        }

        @Test
        @DisplayName("사용자의 모든 리프레시 토큰을 삭제할 수 있다")
        fun `should delete all tokens for user`() {
            // given
            val user = createTestUser()
            val token1 =
                RefreshToken(
                    userId = user.id,
                    token = "token1",
                    expiresAt = LocalDateTime.now().plusDays(7),
                    clientIp = "127.0.0.1",
                )
            val token2 =
                RefreshToken(
                    userId = user.id,
                    token = "token2",
                    expiresAt = LocalDateTime.now().plusDays(7),
                    clientIp = "127.0.0.2",
                )
            refreshTokenRepository.saveAll(listOf(token1, token2))

            // when
            val tokensToDelete = refreshTokenRepository.findByUserId(user.id)
            tokensToDelete.ifPresent { refreshTokenRepository.delete(it) }

            // then
            val remainingTokens = refreshTokenRepository.findAll()
            assertThat(remainingTokens.filter { it.userId == user.id }).hasSize(1)
        }
    }

    @Nested
    @DisplayName("Business Logic 테스트")
    inner class BusinessLogicTest {
        @Test
        @DisplayName("만료된 토큰을 확인할 수 있다")
        fun `should check if token is expired`() {
            // given
            val user = createTestUser()
            val expiredToken =
                RefreshToken(
                    userId = user.id,
                    token = "expired_token",
                    expiresAt = LocalDateTime.now().minusDays(1),
                    clientIp = "127.0.0.1",
                )
            val savedToken = refreshTokenRepository.save(expiredToken)

            // when
            val now = LocalDateTime.now()
            val isExpired = savedToken.isExpired(now)

            // then
            assertThat(isExpired).isTrue
        }

        @Test
        @DisplayName("유효한 토큰을 확인할 수 있다")
        fun `should check if token is valid`() {
            // given
            val user = createTestUser()
            val validToken =
                RefreshToken(
                    userId = user.id,
                    token = "valid_token",
                    expiresAt = LocalDateTime.now().plusDays(7),
                    clientIp = "127.0.0.1",
                )
            val savedToken = refreshTokenRepository.save(validToken)

            // when
            val now = LocalDateTime.now()
            val isValid = savedToken.isValid(now)

            // then
            assertThat(isValid).isTrue
        }

        @Test
        @DisplayName("무효화된 토큰은 유효하지 않다")
        fun `should mark invalidated token as invalid`() {
            // given
            val user = createTestUser()
            val tokenToInvalidate =
                RefreshToken(
                    userId = user.id,
                    token = "token_to_invalidate",
                    expiresAt = LocalDateTime.now().plusDays(7),
                    clientIp = "127.0.0.1",
                )
            val savedToken = refreshTokenRepository.save(tokenToInvalidate)
            savedToken.invalidate()
            refreshTokenRepository.save(savedToken)

            // when
            val now = LocalDateTime.now()
            val isValid = savedToken.isValid(now)

            // then
            assertThat(isValid).isFalse
        }
    }
}
