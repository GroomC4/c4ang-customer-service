package com.groom.customer.application.service

import com.groom.customer.adapter.inbound.web.dto.UserInternalDto
import com.groom.customer.common.annotation.UnitTest
import com.groom.customer.common.exception.UserException
import com.groom.customer.domain.model.User
import com.groom.customer.domain.model.UserProfile
import com.groom.customer.domain.model.UserRole
import com.groom.customer.domain.port.LoadUserPort
import com.groom.customer.domain.service.UserInternalMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

@UnitTest
@DisplayName("InternalUserService 단위 테스트")
class InternalUserServiceTest {
    private val loadUserPort: LoadUserPort = mockk()
    private val userInternalMapper: UserInternalMapper = mockk()
    private val service = InternalUserService(loadUserPort, userInternalMapper)

    @Nested
    @DisplayName("getUserById 테스트")
    inner class GetUserByIdTest {
        @Test
        @DisplayName("사용자 ID로 사용자를 조회하여 DTO 응답을 반환한다")
        fun `should get user by id and return dto response`() {
            // given
            val userId = UUID.randomUUID()
            val user =
                User(
                    username = "테스트사용자",
                    email = "test@example.com",
                    passwordHash = "hashedPassword",
                    role = UserRole.CUSTOMER,
                ).apply {
                    id = userId
                    profile =
                        UserProfile(
                            fullName = "테스트 사용자",
                            phoneNumber = "010-1234-5678",
                            contactEmail = "test@example.com",
                            defaultAddress = null,
                        )
                }

            val dtoResponse =
                UserInternalDto(
                    userId = userId.toString(),
                    username = "테스트사용자",
                    email = "test@example.com",
                    role = "CUSTOMER",
                    isActive = true,
                    profile =
                        com.groom.customer.adapter.inbound.web.dto.UserProfileDto(
                            fullName = "테스트 사용자",
                            phoneNumber = "010-1234-5678",
                            address = null,
                        ),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    lastLoginAt = null,
                )

            every { loadUserPort.loadById(userId) } returns user
            every { userInternalMapper.toUserInternalDto(user) } returns dtoResponse

            // when
            val result = service.getUserById(userId)

            // then
            assertThat(result).isNotNull
            assertThat(result.userId).isEqualTo(userId.toString())
            assertThat(result.username).isEqualTo("테스트사용자")
            assertThat(result.email).isEqualTo("test@example.com")
            assertThat(result.role).isEqualTo("CUSTOMER")
            assertThat(result.isActive).isTrue()
            assertThat(result.profile.fullName).isEqualTo("테스트 사용자")
            assertThat(result.profile.phoneNumber).isEqualTo("010-1234-5678")

            verify(exactly = 1) { loadUserPort.loadById(userId) }
            verify(exactly = 1) { userInternalMapper.toUserInternalDto(user) }
        }

        @Test
        @DisplayName("존재하지 않는 사용자 ID로 조회 시 UserNotFoundException을 던진다")
        fun `should throw UserNotFoundException when user id not found`() {
            // given
            val userId = UUID.randomUUID()
            every { loadUserPort.loadById(userId) } returns null

            // when & then
            assertThatThrownBy { service.getUserById(userId) }
                .isInstanceOf(UserException.UserNotFound::class.java)
                .hasMessageContaining(userId.toString())

            verify(exactly = 1) { loadUserPort.loadById(userId) }
            verify(exactly = 0) { userInternalMapper.toUserInternalDto(any()) }
        }
    }
}
