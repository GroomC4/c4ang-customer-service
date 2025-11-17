package com.groom.customer.adapter.outbound.persistence

import com.groom.customer.common.annotation.UnitTest
import com.groom.customer.domain.model.User
import com.groom.customer.domain.model.UserRole
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID

@UnitTest
@DisplayName("UserPersistenceAdapter 단위 테스트")
class UserPersistenceAdapterTest {
    private val userRepository: UserRepository = mockk()
    private val adapter = UserPersistenceAdapter(userRepository)

    @Nested
    @DisplayName("loadByEmail 테스트")
    inner class LoadByEmailTest {
        @Test
        @DisplayName("이메일로 사용자를 조회할 수 있다")
        fun `should load user by email`() {
            // given
            val email = "test@example.com"
            val user =
                User(
                    username = "테스트",
                    email = email,
                    passwordHash = "password",
                    role = UserRole.CUSTOMER,
                )
            every { userRepository.findByEmail(email) } returns Optional.of(user)

            // when
            val result = adapter.loadByEmail(email)

            // then
            assertThat(result).isNotNull
            assertThat(result?.email).isEqualTo(email)
            verify(exactly = 1) { userRepository.findByEmail(email) }
        }

        @Test
        @DisplayName("존재하지 않는 이메일 조회 시 null을 반환한다")
        fun `should return null when email not found`() {
            // given
            val email = "notexist@example.com"
            every { userRepository.findByEmail(email) } returns Optional.empty()

            // when
            val result = adapter.loadByEmail(email)

            // then
            assertThat(result).isNull()
            verify(exactly = 1) { userRepository.findByEmail(email) }
        }
    }

    @Nested
    @DisplayName("loadByEmailAndRole 테스트")
    inner class LoadByEmailAndRoleTest {
        @Test
        @DisplayName("이메일과 역할로 사용자를 조회할 수 있다")
        fun `should load user by email and role`() {
            // given
            val email = "test@example.com"
            val role = UserRole.CUSTOMER
            val user =
                User(
                    username = "테스트",
                    email = email,
                    passwordHash = "password",
                    role = role,
                )
            every { userRepository.findByEmailAndRole(email, role) } returns Optional.of(user)

            // when
            val result = adapter.loadByEmailAndRole(email, role)

            // then
            assertThat(result).isNotNull
            assertThat(result?.email).isEqualTo(email)
            assertThat(result?.role).isEqualTo(role)
            verify(exactly = 1) { userRepository.findByEmailAndRole(email, role) }
        }

        @Test
        @DisplayName("같은 이메일이라도 역할이 다르면 null을 반환한다")
        fun `should return null when role does not match`() {
            // given
            val email = "test@example.com"
            every { userRepository.findByEmailAndRole(email, UserRole.OWNER) } returns Optional.empty()

            // when
            val result = adapter.loadByEmailAndRole(email, UserRole.OWNER)

            // then
            assertThat(result).isNull()
            verify(exactly = 1) { userRepository.findByEmailAndRole(email, UserRole.OWNER) }
        }
    }

    @Nested
    @DisplayName("loadById 테스트")
    inner class LoadByIdTest {
        @Test
        @DisplayName("ID로 사용자를 조회할 수 있다")
        fun `should load user by id`() {
            // given
            val userId = UUID.randomUUID()
            val user =
                User(
                    username = "테스트",
                    email = "test@example.com",
                    passwordHash = "password",
                    role = UserRole.CUSTOMER,
                )
            every { userRepository.findById(userId) } returns Optional.of(user)

            // when
            val result = adapter.loadById(userId)

            // then
            assertThat(result).isNotNull
            assertThat(result?.username).isEqualTo("테스트")
            verify(exactly = 1) { userRepository.findById(userId) }
        }

        @Test
        @DisplayName("존재하지 않는 ID 조회 시 null을 반환한다")
        fun `should return null when id not found`() {
            // given
            val userId = UUID.randomUUID()
            every { userRepository.findById(userId) } returns Optional.empty()

            // when
            val result = adapter.loadById(userId)

            // then
            assertThat(result).isNull()
            verify(exactly = 1) { userRepository.findById(userId) }
        }
    }

    @Nested
    @DisplayName("save 테스트")
    inner class SaveTest {
        @Test
        @DisplayName("사용자를 저장할 수 있다")
        fun `should save user`() {
            // given
            val user =
                User(
                    username = "테스트",
                    email = "test@example.com",
                    passwordHash = "password",
                    role = UserRole.CUSTOMER,
                )
            every { userRepository.save(user) } returns user

            // when
            val result = adapter.save(user)

            // then
            assertThat(result).isEqualTo(user)
            verify(exactly = 1) { userRepository.save(user) }
        }

        @Test
        @DisplayName("비활성화된 사용자를 저장할 수 있다")
        fun `should save deactivated user`() {
            // given
            val user =
                User(
                    username = "테스트",
                    email = "test@example.com",
                    passwordHash = "password",
                    role = UserRole.CUSTOMER,
                    isActive = false,
                )
            every { userRepository.save(user) } returns user

            // when
            val result = adapter.save(user)

            // then
            assertThat(result.isActive).isFalse
            verify(exactly = 1) { userRepository.save(user) }
        }
    }
}
