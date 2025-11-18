package com.groom.customer.application.service

import com.groom.customer.common.annotation.UnitTest
import com.groom.customer.common.exception.UserException
import com.groom.customer.domain.model.User
import com.groom.customer.domain.model.UserProfile
import com.groom.customer.domain.model.UserRole
import com.groom.customer.domain.port.LoadUserPort
import com.groom.customer.domain.service.UserInternalMapper
import com.groom.ecommerce.customer.api.avro.UserInternalResponse
import com.groom.ecommerce.customer.api.avro.UserProfileInternal
import com.groom.ecommerce.customer.api.avro.UserRole as AvroUserRole
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
        @DisplayName("사용자 ID로 사용자를 조회하여 Avro 응답을 반환한다")
        fun `should get user by id and return avro response`() {
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

            val avroResponse =
                UserInternalResponse
                    .newBuilder()
                    .setUserId(userId.toString())
                    .setUsername("테스트사용자")
                    .setEmail("test@example.com")
                    .setRole(AvroUserRole.CUSTOMER)
                    .setIsActive(true)
                    .setProfile(
                        UserProfileInternal
                            .newBuilder()
                            .setFullName("테스트 사용자")
                            .setPhoneNumber("010-1234-5678")
                            .setAddress(null)
                            .build(),
                    ).setCreatedAt(System.currentTimeMillis())
                    .setUpdatedAt(System.currentTimeMillis())
                    .setLastLoginAt(null)
                    .build()

            every { loadUserPort.loadById(userId) } returns user
            every { userInternalMapper.toUserInternalResponse(user) } returns avroResponse

            // when
            val result = service.getUserById(userId)

            // then
            assertThat(result).isNotNull
            assertThat(result.userId).isEqualTo(userId.toString())
            assertThat(result.username).isEqualTo("테스트사용자")
            assertThat(result.email).isEqualTo("test@example.com")
            assertThat(result.role).isEqualTo(AvroUserRole.CUSTOMER)
            assertThat(result.isActive).isTrue()
            assertThat(result.profile.fullName).isEqualTo("테스트 사용자")
            assertThat(result.profile.phoneNumber).isEqualTo("010-1234-5678")

            verify(exactly = 1) { loadUserPort.loadById(userId) }
            verify(exactly = 1) { userInternalMapper.toUserInternalResponse(user) }
        }

        @Test
        @DisplayName("OWNER 역할 사용자를 조회할 수 있다")
        fun `should get owner user by id`() {
            // given
            val userId = UUID.randomUUID()
            val user =
                User(
                    username = "사장님",
                    email = "owner@example.com",
                    passwordHash = "hashedPassword",
                    role = UserRole.OWNER,
                ).apply {
                    id = userId
                    profile =
                        UserProfile(
                            fullName = "가게 사장님",
                            phoneNumber = "010-9999-8888",
                            contactEmail = "owner@example.com",
                            defaultAddress = null,
                        )
                }

            val avroResponse =
                UserInternalResponse
                    .newBuilder()
                    .setUserId(userId.toString())
                    .setUsername("사장님")
                    .setEmail("owner@example.com")
                    .setRole(AvroUserRole.OWNER)
                    .setIsActive(true)
                    .setProfile(
                        UserProfileInternal
                            .newBuilder()
                            .setFullName("가게 사장님")
                            .setPhoneNumber("010-9999-8888")
                            .setAddress(null)
                            .build(),
                    ).setCreatedAt(System.currentTimeMillis())
                    .setUpdatedAt(System.currentTimeMillis())
                    .setLastLoginAt(null)
                    .build()

            every { loadUserPort.loadById(userId) } returns user
            every { userInternalMapper.toUserInternalResponse(user) } returns avroResponse

            // when
            val result = service.getUserById(userId)

            // then
            assertThat(result.role).isEqualTo(AvroUserRole.OWNER)
            verify(exactly = 1) { loadUserPort.loadById(userId) }
            verify(exactly = 1) { userInternalMapper.toUserInternalResponse(user) }
        }

        @Test
        @DisplayName("MANAGER 역할은 ADMIN으로 매핑되어 반환된다")
        fun `should map manager role to admin`() {
            // given
            val userId = UUID.randomUUID()
            val user =
                User(
                    username = "매니저",
                    email = "manager@example.com",
                    passwordHash = "hashedPassword",
                    role = UserRole.MANAGER,
                ).apply {
                    id = userId
                    profile =
                        UserProfile(
                            fullName = "관리자",
                            phoneNumber = "010-5555-6666",
                            contactEmail = "manager@example.com",
                            defaultAddress = null,
                        )
                }

            val avroResponse =
                UserInternalResponse
                    .newBuilder()
                    .setUserId(userId.toString())
                    .setUsername("매니저")
                    .setEmail("manager@example.com")
                    .setRole(AvroUserRole.ADMIN) // MANAGER → ADMIN 매핑
                    .setIsActive(true)
                    .setProfile(
                        UserProfileInternal
                            .newBuilder()
                            .setFullName("관리자")
                            .setPhoneNumber("010-5555-6666")
                            .setAddress(null)
                            .build(),
                    ).setCreatedAt(System.currentTimeMillis())
                    .setUpdatedAt(System.currentTimeMillis())
                    .setLastLoginAt(null)
                    .build()

            every { loadUserPort.loadById(userId) } returns user
            every { userInternalMapper.toUserInternalResponse(user) } returns avroResponse

            // when
            val result = service.getUserById(userId)

            // then
            assertThat(result.role).isEqualTo(AvroUserRole.ADMIN)
            verify(exactly = 1) { loadUserPort.loadById(userId) }
            verify(exactly = 1) { userInternalMapper.toUserInternalResponse(user) }
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
            verify(exactly = 0) { userInternalMapper.toUserInternalResponse(any()) }
        }
    }
}
