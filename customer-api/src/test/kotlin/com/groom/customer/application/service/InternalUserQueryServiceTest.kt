package com.groom.customer.application.service

import com.groom.customer.common.annotation.UnitTest
import com.groom.customer.domain.model.User
import com.groom.customer.domain.model.UserRole
import com.groom.customer.domain.port.LoadUserPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

@UnitTest
@DisplayName("InternalUserQueryService 단위 테스트")
class InternalUserQueryServiceTest {
    private val loadUserPort: LoadUserPort = mockk()
    private val service = InternalUserQueryService(loadUserPort)

    @Nested
    @DisplayName("getUserById 테스트")
    inner class GetUserByIdTest {
        @Test
        @DisplayName("사용자 ID로 사용자를 조회할 수 있다")
        fun `should get user by id`() {
            // given
            val userId = UUID.randomUUID()
            val user =
                User(
                    username = "테스트사용자",
                    email = "test@example.com",
                    passwordHash = "hashedPassword",
                    role = UserRole.CUSTOMER,
                )
            every { loadUserPort.loadById(userId) } returns user

            // when
            val result = service.getUserById(userId)

            // then
            assertThat(result).isNotNull
            assertThat(result?.username).isEqualTo("테스트사용자")
            assertThat(result?.email).isEqualTo("test@example.com")
            verify(exactly = 1) { loadUserPort.loadById(userId) }
        }

        @Test
        @DisplayName("존재하지 않는 사용자 ID로 조회 시 null을 반환한다")
        fun `should return null when user id not found`() {
            // given
            val userId = UUID.randomUUID()
            every { loadUserPort.loadById(userId) } returns null

            // when
            val result = service.getUserById(userId)

            // then
            assertThat(result).isNull()
            verify(exactly = 1) { loadUserPort.loadById(userId) }
        }
    }
}
