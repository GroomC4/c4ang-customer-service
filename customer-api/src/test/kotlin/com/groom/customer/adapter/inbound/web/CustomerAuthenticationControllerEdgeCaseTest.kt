package com.groom.customer.adapter.inbound.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.groom.customer.adapter.inbound.web.dto.LoginRequest
import com.groom.customer.adapter.inbound.web.dto.SignupCustomerRequest
import org.springframework.boot.test.context.SpringBootTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlGroup
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("고객 인증 컨트롤러 예외 상황 테스트")
@SpringBootTest(properties = ["spring.profiles.active=test"])
@AutoConfigureMockMvc
@SqlGroup(
    Sql(scripts = ["/sql/integration/customer-auth-test-data.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
    Sql(scripts = ["/sql/integration/cleanup.sql"], executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD),
)
class CustomerAuthenticationControllerEdgeCaseTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Nested
    @DisplayName("회원가입 검증 테스트")
    inner class SignupValidationTest {
        @Test
        @DisplayName("잘못된 이메일 형식으로 회원가입 시 400 Bad Request를 반환한다")
        fun `should return 400 when email format is invalid`() {
            // given
            val request =
                SignupCustomerRequest(
                    username = "홍길동",
                    email = "invalid-email",
                    password = "ValidPass123!",
                    defaultPhoneNumber = "010-1234-5678",
                    defaultAddress = "서울시 강남구",
                )

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/auth/customers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").exists())
        }

        @Test
        @DisplayName("이미 존재하는 이메일로 회원가입 시 409 Conflict를 반환한다")
        fun `should return 409 when email already exists`() {
            // given - customer@example.com은 SQL 스크립트로 이미 존재
            val request =
                SignupCustomerRequest(
                    username = "신규고객",
                    email = "customer@example.com",
                    password = "NewPass123!",
                    defaultPhoneNumber = "010-9999-9999",
                    defaultAddress = "서울시 강남구",
                )

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/auth/customers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"))
        }

    }

    @Nested
    @DisplayName("로그인 검증 테스트")
    inner class LoginValidationTest {
        @Test
        @DisplayName("존재하지 않는 이메일로 로그인 시 401 Unauthorized를 반환한다")
        fun `should return 401 when email does not exist`() {
            // given
            val request =
                LoginRequest(
                    email = "notexist@example.com",
                    password = "AnyPassword123!",
                )

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/auth/customers/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND_BY_EMAIL"))
        }

        @Test
        @DisplayName("잘못된 비밀번호로 로그인 시 401 Unauthorized를 반환한다")
        fun `should return 401 when password is wrong`() {
            // given
            val request =
                LoginRequest(
                    email = "customer@example.com",
                    password = "WrongPassword123!",
                )

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/auth/customers/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"))
        }

        @Test
        @DisplayName("비활성화된 계정으로 로그인 시 401 Unauthorized를 반환한다")
        fun `should return 401 when account is deactivated`() {
            // given - inactive_customer@example.com은 비활성화된 계정
            val request =
                LoginRequest(
                    email = "inactive_customer@example.com",
                    password = "password123!",
                )

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/auth/customers/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND_BY_EMAIL"))
        }

        @Test
        @DisplayName("잘못된 이메일 형식으로 로그인 시 401 Unauthorized를 반환한다")
        fun `should return 401 when email format is invalid in login`() {
            // given
            val request =
                LoginRequest(
                    email = "invalid-email-format",
                    password = "password123!",
                )

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/auth/customers/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND_BY_EMAIL"))
        }

        @Test
        @DisplayName("빈 이메일로 로그인 시 401 Unauthorized를 반환한다")
        fun `should return 401 when email is empty`() {
            // given
            val request =
                LoginRequest(
                    email = "",
                    password = "password123!",
                )

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/auth/customers/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND_BY_EMAIL"))
        }

        @Test
        @DisplayName("빈 비밀번호로 로그인 시 401 Unauthorized를 반환한다")
        fun `should return 401 when password is empty in login`() {
            // given
            val request =
                LoginRequest(
                    email = "customer@example.com",
                    password = "",
                )

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/auth/customers/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"))
        }
    }

    @Nested
    @DisplayName("역할 검증 테스트")
    inner class RoleValidationTest {
        @Test
        @DisplayName("OWNER 계정으로 CUSTOMER 로그인 시도 시 401 Unauthorized를 반환한다")
        fun `should return 401 when trying to login as customer with owner account`() {
            // given - owner@example.com은 OWNER 역할
            val request =
                LoginRequest(
                    email = "owner@example.com",
                    password = "password123!",
                )

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/auth/customers/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND_BY_EMAIL"))
        }
    }

    @Nested
    @DisplayName("로그아웃 테스트")
    inner class LogoutTest {
        @Test
        @DisplayName("잘못된 UUID 형식의 헤더로 로그아웃 시 400 Bad Request를 반환한다")
        fun `should return 400 when user id header has invalid uuid format`() {
            // when & then
            mockMvc
                .perform(
                    post("/api/v1/auth/customers/logout")
                        .header("X-User-Id", "invalid-uuid")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
        }

        @Test
        @DisplayName("User ID 헤더 없이 로그아웃 시 400 Bad Request를 반환한다")
        fun `should return 400 when user id header is missing`() {
            // when & then
            mockMvc
                .perform(
                    post("/api/v1/auth/customers/logout")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
        }
    }
}
