package com.groom.customer.adapter.outbound.persistence

import com.groom.customer.common.TransactionApplier
import com.groom.customer.common.annotation.IntegrationTest
import com.groom.customer.domain.model.RefreshToken
import com.groom.customer.domain.model.User
import com.groom.customer.domain.model.UserRole
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.UUID

@IntegrationTest
@DisplayName("RefreshTokenRepository CRUD 테스트")
class RefreshTokenRepositoryTest {
    @Autowired
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var transactionApplier: TransactionApplier

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
        val savedUser = userRepository.save(user)
        userRepository.flush()
        return savedUser
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
            refreshTokenRepository.flush()

            // then
            assertThat(savedToken.id).isNotNull
            assertThat(savedToken.userId).isEqualTo(user.id)
            assertThat(savedToken.token).isEqualTo("refresh_token_value")
            assertThat(savedToken.token).isNotNull()
        }

        @Test
        @DisplayName("같은 사용자의 기존 토큰은 업데이트하여 재사용한다")
        fun `should update existing token for same user`() {
            // given
            val user = createTestUser()
            val oldToken =
                RefreshToken(
                    userId = user.id,
                    token = "old_token",
                    expiresAt = LocalDateTime.now().plusDays(7),
                    clientIp = "127.0.0.1",
                )
            val savedToken = refreshTokenRepository.saveAndFlush(oldToken)

            // when - 같은 엔티티를 업데이트
            savedToken.updateToken("new_token", LocalDateTime.now().plusDays(14))
            refreshTokenRepository.saveAndFlush(savedToken)

            // then - user_id는 UNIQUE 제약조건이므로 한 사용자당 하나의 토큰만 존재
            val tokens =
                transactionApplier.applyPrimaryTransaction {
                    refreshTokenRepository.findAll()
                }
            assertThat(tokens).hasSize(1)
            assertThat(tokens[0].token).isEqualTo("new_token")
            assertThat(tokens[0].userId).isEqualTo(user.id)
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
            refreshTokenRepository.flush()

            // when
            val foundToken =
                transactionApplier.applyPrimaryTransaction {
                    refreshTokenRepository.findByUserId(user.id)
                }

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
            refreshTokenRepository.flush()

            // when
            val foundToken =
                transactionApplier.applyPrimaryTransaction {
                    refreshTokenRepository.findByToken("unique_token_value")
                }

            // then
            assertThat(foundToken).isPresent
            assertThat(foundToken.get().userId).isEqualTo(user.id)
        }

        @Test
        @DisplayName("존재하지 않는 토큰 조회 시 빈 Optional을 반환한다")
        fun `should return empty optional when token not found`() {
            // when
            val foundToken =
                transactionApplier.applyPrimaryTransaction {
                    refreshTokenRepository.findByToken("nonexistent_token")
                }

            // then
            assertThat(foundToken).isEmpty
        }

        @Test
        @DisplayName("존재하지 않는 사용자 ID로 조회 시 빈 Optional을 반환한다")
        fun `should return empty optional when user id not found`() {
            // when
            val foundToken =
                transactionApplier.applyPrimaryTransaction {
                    refreshTokenRepository.findByUserId(UUID.randomUUID())
                }

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
            refreshTokenRepository.flush()
            val tokenId = savedToken.id

            // when
            savedToken.invalidate()
            refreshTokenRepository.save(savedToken)
            refreshTokenRepository.flush() // 즉시 DB에 반영
            entityManager.clear() // 영속성 컨텍스트 초기화

            // then - Primary DB에서 실제 반영된 데이터 검증
            val foundToken =
                transactionApplier.applyPrimaryTransaction {
                    refreshTokenRepository.findById(tokenId).get()
                }
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
            refreshTokenRepository.flush()
            val tokenId = savedToken.id

            // when
            val newExpiry = LocalDateTime.now().plusDays(14)
            savedToken.updateToken("new_token", newExpiry)
            refreshTokenRepository.save(savedToken)
            refreshTokenRepository.flush() // 즉시 DB에 반영
            entityManager.clear() // 영속성 컨텍스트 초기화

            // then - Primary DB에서 실제 반영된 데이터 검증
            val foundToken =
                transactionApplier.applyPrimaryTransaction {
                    refreshTokenRepository.findById(tokenId).get()
                }
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
            refreshTokenRepository.flush()
            val tokenId = savedToken.id

            // when
            refreshTokenRepository.delete(savedToken)
            refreshTokenRepository.flush() // 즉시 DB에 반영
            entityManager.clear() // 영속성 컨텍스트 초기화

            // then - Primary DB에서 실제 삭제 확인
            val foundToken =
                transactionApplier.applyPrimaryTransaction {
                    refreshTokenRepository.findById(tokenId)
                }
            assertThat(foundToken).isEmpty
        }

        @Test
        @DisplayName("사용자 ID로 리프레시 토큰을 삭제할 수 있다")
        fun `should delete refresh token by user id`() {
            // given - 각각 다른 사용자 생성
            val user1 = createTestUser()
            val user2 =
                User(
                    username = "테스트유저2",
                    email = "test2@example.com",
                    passwordHash = "password",
                    role = UserRole.CUSTOMER,
                )
            userRepository.save(user2)
            userRepository.flush()

            val token1 =
                RefreshToken(
                    userId = user1.id,
                    token = "token1",
                    expiresAt = LocalDateTime.now().plusDays(7),
                    clientIp = "127.0.0.1",
                )
            val token2 =
                RefreshToken(
                    userId = user2.id,
                    token = "token2",
                    expiresAt = LocalDateTime.now().plusDays(7),
                    clientIp = "127.0.0.2",
                )
            refreshTokenRepository.saveAll(listOf(token1, token2))
            refreshTokenRepository.flush()

            // when - user1의 토큰만 삭제
            val tokensToDelete =
                transactionApplier.applyPrimaryTransaction {
                    refreshTokenRepository.findByUserId(user1.id)
                }
            tokensToDelete.ifPresent { refreshTokenRepository.delete(it) }
            refreshTokenRepository.flush() // 즉시 DB에 반영
            entityManager.clear() // 영속성 컨텍스트 초기화

            // then - Primary DB에서 실제 삭제 확인: user1의 토큰은 삭제되고 user2의 토큰만 남음
            val remainingTokens =
                transactionApplier.applyPrimaryTransaction {
                    refreshTokenRepository.findAll()
                }
            assertThat(remainingTokens).hasSize(1)
            assertThat(remainingTokens[0].userId).isEqualTo(user2.id)
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
            refreshTokenRepository.flush()

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
            refreshTokenRepository.flush()

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
            refreshTokenRepository.flush()

            // when
            val now = LocalDateTime.now()
            val isValid = savedToken.isValid(now)

            // then
            assertThat(isValid).isFalse
        }
    }
}
