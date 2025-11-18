package com.groom.customer.adapter.inbound.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.groom.customer.common.IntegrationTestBase
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlGroup
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("InternalUserController 통합 테스트")
@AutoConfigureMockMvc
@SqlGroup(
    Sql(scripts = ["/sql/integration/customer-auth-test-data.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
    Sql(scripts = ["/sql/integration/cleanup.sql"], executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD),
)
class InternalUserControllerIntegrationTest : IntegrationTestBase() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Nested
    @DisplayName("GET /internal/v1/users/{userId} - ID로 사용자 조회")
    inner class GetUserByIdTest {
        @Test
        @DisplayName("유효한 사용자 ID로 조회 시 200 OK와 사용자 정보를 반환한다")
        fun `should return 200 and user data when user id exists`() {
            // given
            val userId = "750e8400-e29b-41d4-a716-446655440001" // customer@example.com

            // when & then
            mockMvc
                .perform(
                    get("/internal/v1/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.username").value("고객테스트"))
                .andExpect(jsonPath("$.email").value("customer@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.profile").exists())
                .andExpect(jsonPath("$.profile.fullName").value("고객테스트"))
                .andExpect(jsonPath("$.profile.phoneNumber").value("010-1111-2222"))
                .andExpect(jsonPath("$.profile.address").isEmpty)
                .andExpect(jsonPath("$.createdAt").isNumber)
                .andExpect(jsonPath("$.updatedAt").isNumber)
        }

        @Test
        @DisplayName("비활성화된 사용자도 조회할 수 있다")
        fun `should return inactive user when user id exists`() {
            // given
            val userId = "750e8400-e29b-41d4-a716-446655440004" // inactive_customer@example.com

            // when & then
            mockMvc
                .perform(
                    get("/internal/v1/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.username").value("비활성고객"))
                .andExpect(jsonPath("$.email").value("inactive_customer@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.isActive").value(false))
                .andExpect(jsonPath("$.profile.fullName").value("비활성고객"))
                .andExpect(jsonPath("$.profile.phoneNumber").value("010-5555-6666"))
        }

        @Test
        @DisplayName("OWNER 역할 사용자도 조회할 수 있다")
        fun `should return owner user when user id exists`() {
            // given
            val userId = "750e8400-e29b-41d4-a716-446655440005" // owner@example.com

            // when & then
            mockMvc
                .perform(
                    get("/internal/v1/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.username").value("사장님"))
                .andExpect(jsonPath("$.email").value("owner@example.com"))
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.profile.fullName").value("사장님"))
                .andExpect(jsonPath("$.profile.phoneNumber").value("010-7777-8888"))
        }

        @Test
        @DisplayName("존재하지 않는 사용자 ID로 조회 시 404 NOT_FOUND를 반환한다")
        fun `should return 404 when user id not found`() {
            // given
            val nonExistentUserId = "00000000-0000-0000-0000-000000000000"

            // when & then
            mockMvc
                .perform(
                    get("/internal/v1/users/{userId}", nonExistentUserId)
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists())
        }

        @Test
        @DisplayName("잘못된 형식의 UUID로 조회 시 400 BAD_REQUEST를 반환한다")
        fun `should return 400 when user id format is invalid`() {
            // given
            val invalidUserId = "invalid-uuid-format"

            // when & then
            mockMvc
                .perform(
                    get("/internal/v1/users/{userId}", invalidUserId)
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
        }

        @Test
        @DisplayName("타임스탬프 필드가 epoch milliseconds로 반환된다")
        fun `should return timestamp fields as epoch milliseconds`() {
            // given
            val userId = "750e8400-e29b-41d4-a716-446655440001" // customer@example.com

            // when & then
            mockMvc
                .perform(
                    get("/internal/v1/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.createdAt").isNumber)
                .andExpect(jsonPath("$.updatedAt").isNumber)
                .andExpect(jsonPath("$.createdAt").value(org.hamcrest.Matchers.greaterThan(0L)))
                .andExpect(jsonPath("$.updatedAt").value(org.hamcrest.Matchers.greaterThan(0L)))
        }
    }
}
