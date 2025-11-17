package com.groom.customer.adapter.inbound.web

import com.auth0.jwt.JWT
import com.fasterxml.jackson.databind.ObjectMapper
import com.groom.customer.adapter.inbound.web.dto.LoginRequest
import com.groom.customer.adapter.inbound.web.dto.RegisterOwnerRequest
import com.groom.customer.adapter.outbound.persistence.UserRepository
import com.groom.customer.common.annotation.IntegrationTest
import com.groom.customer.configuration.jwt.JwtProperties
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlGroup
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("판매자 인증 컨트롤러 통합 테스트")
@IntegrationTest
@SpringBootTest
@AutoConfigureMockMvc
@SqlGroup(
    Sql(scripts = ["/sql/integration/owner-auth-test-data.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
    Sql(scripts = ["/sql/integration/cleanup.sql"], executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD),
)
class OwnerAuthenticationControllerIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jwtProperties: JwtProperties

    @Nested
    @DisplayName("판매자 로그인 API 테스트")
    inner class OwnerLoginTests {
        @Test
        @DisplayName("POST /api/v1/auth/owners/login - 정상적인 로그인 요청 시 200 OK와 토큰 정보를 반환한다")
        fun testSuccessfulLogin() {
            // given - SQL로 사용자 데이터 준비됨
            val loginRequest =
                LoginRequest(
                    email = "owner@example.com",
                    password = "password123!",
                )

            // when & then
            val response =
                mockMvc
                    .perform(
                        post("/api/v1/auth/owners/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)),
                    ).andExpect(status().isOk)
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.accessToken").isString)
                    .andExpect(jsonPath("$.refreshToken").exists())
                    .andExpect(jsonPath("$.refreshToken").isString)
                    .andExpect(jsonPath("$.expiresIn").value(300))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andReturn()

            // 응답 데이터 추가 검증: JWT 토큰 구조 확인
            val responseBody = objectMapper.readTree(response.response.contentAsString)
            val accessToken = responseBody.get("accessToken").asText()
            val refreshToken = responseBody.get("refreshToken").asText()

            // Access Token 검증
            val decodedAccessToken = JWT.decode(accessToken)
            assert(decodedAccessToken.subject != null) { "Access token must have subject" }
            assert(decodedAccessToken.getClaim("role").asString() == "OWNER") { "Role must be OWNER" }
            assert(decodedAccessToken.issuer == jwtProperties.issuer) { "Issuer must match" }

            // Refresh Token 검증
            val decodedRefreshToken = JWT.decode(refreshToken)
            assert(decodedRefreshToken.subject != null) { "Refresh token must have subject" }
            assert(decodedRefreshToken.getClaim("role").asString() == "OWNER") { "Role must be OWNER" }
            assert(decodedRefreshToken.issuer == jwtProperties.issuer) { "Issuer must match" }
        }

        @Test
        @DisplayName("POST /api/v1/auth/owners/login - 잘못된 비밀번호로 로그인 시도 시 401 Unauthorized를 반환한다")
        fun testLoginWithWrongPassword() {
            // given - SQL로 사용자 데이터 준비됨
            val loginRequest =
                LoginRequest(
                    email = "ownerwrongpwd@example.com",
                    password = "wrongPassword",
                )

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/auth/owners/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)),
                ).andExpect(status().isUnauthorized)
        }

        @Test
        @DisplayName("POST /api/v1/auth/owners/login - 존재하지 않는 이메일로 로그인 시도 시 401 Unauthorized를 반환한다")
        fun testLoginWithNonExistentEmail() {
            // given
            val loginRequest =
                LoginRequest(
                    email = "notexistowner@example.com",
                    password = "password123",
                )

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/auth/owners/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)),
                ).andExpect(status().isUnauthorized)
        }
    }

    @Nested
    @DisplayName("판매자 로그아웃 API 테스트")
    inner class OwnerLogoutTests {
        @Test
        @DisplayName("POST /api/v1/auth/owners/logout - 로그인한 판매자가 로그아웃하면 204 No Content를 반환한다")
        fun testSuccessfulLogout() {
            // given - SQL로 사용자 데이터 준비됨
            // 로그인하여 access token 획득
            val loginRequest =
                LoginRequest(
                    email = "logoutowner@example.com",
                    password = "password123!",
                )

            val loginResponse =
                mockMvc
                    .perform(
                        post("/api/v1/auth/owners/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)),
                    ).andExpect(status().isOk)
                    .andReturn()
                    .response
                    .contentAsString

            val loginResponseBody = objectMapper.readTree(loginResponse)
            val accessToken = loginResponseBody.get("accessToken").asText()

            // JWT 토큰에서 userId 추출 (실제 환경에서는 Istio가 처리)
            val decodedJWT = JWT.decode(accessToken)
            val userId = decodedJWT.subject

            // when & then
            // Istio가 JWT를 검증하고 X-User-Id 헤더를 주입한다고 가정
            mockMvc
                .perform(
                    post("/api/v1/auth/owners/logout")
                        .header("X-User-Id", userId),
                ).andExpect(status().isNoContent)
        }

        // JWT 검증 관련 테스트는 제거됨:
        // - Istio API Gateway가 JWT 검증을 담당
        // - 만료된 토큰, 잘못된 서명, 잘못된 issuer, 토큰 없음 등은 Istio에서 처리
        // - 이 서비스는 Istio가 검증한 X-User-Id 헤더만 사용
    }

    @Nested
    @DisplayName("판매자 회원가입 API 테스트")
    inner class OwnerSignupTests {
        @Test
        @DisplayName("POST /api/v1/auth/owners/signup - 정상적인 판매자 회원가입 요청 시 201 Created와 판매자 정보를 반환한다")
        fun testSuccessfulSignup() {
            // given
            val request =
                RegisterOwnerRequest(
                    username = "판매자김",
                    email = "newowner@example.com",
                    password = "P@ssw0rd!",
                    phoneNumber = "010-9999-9999",
                )

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/auth/owners/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.user.id").exists())
                .andExpect(jsonPath("$.user.name").value("판매자김"))
                .andExpect(jsonPath("$.user.email").value("newowner@example.com"))
                .andExpect(jsonPath("$.createdAt").exists())
        }

        @Test
        @DisplayName("POST /api/v1/auth/owners/signup - 판매자 회원가입 성공 시 DB에 User와 UserProfile이 모두 저장된다")
        fun testSignupSavesAllEntities() {
            // given
            val request =
                RegisterOwnerRequest(
                    username = "스토어주인",
                    email = "storeowner@example.com",
                    password = "password123!",
                    phoneNumber = "010-8888-8888",
                )

            // when
            mockMvc
                .perform(
                    post("/api/v1/auth/owners/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isCreated)

            // then - DB에 저장되었는지 확인
            val savedUser = userRepository.findByEmail("storeowner@example.com").orElseThrow()
            assert(savedUser.username == "스토어주인")
            assert(savedUser.role.name == "OWNER")
            assert(savedUser.profile != null)
            assert(savedUser.profile!!.fullName == "스토어주인")
            assert(savedUser.profile!!.phoneNumber == "010-8888-8888")

//            // 스토어 저장 확인
//            val savedStore = storeRepository.findByOwnerUserId(saveduser.id).orElseThrow()
//            assert(savedStore.name == "패션 스토어")
//            assert(savedStore.description == "최신 패션 아이템")
//            assert(savedStore.status == StoreStatus.REGISTERED)
//            assert(savedStore.rating != null)
//            assert(savedStore.rating!!.reviewCount == 0)
        }

        @Test
        @DisplayName("POST /api/v1/auth/owners/signup - 동일한 이메일로 판매자 중복 가입 시도 시 409 Conflict를 반환한다")
        fun testDuplicateEmailSignup() {
            // given - SQL로 사용자 데이터 준비됨 (owner@example.com)
            // 동일 이메일로 가입 시도
            val duplicateRequest =
                RegisterOwnerRequest(
                    username = "두번째판매자",
                    email = "owner@example.com", // SQL에 이미 존재하는 이메일
                    password = "different123!",
                    phoneNumber = "010-6666-6666",
                )

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/auth/owners/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)),
                ).andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"))
                .andExpect(jsonPath("$.message").value("이미 존재하는 이메일입니다: owner@example.com"))
        }

        @Test
        @DisplayName("POST /api/v1/auth/owners/signup - 잘못된 이메일 형식으로 판매자 가입 시도 시 400 Bad Request를 반환한다")
        fun testInvalidEmailFormat() {
            // given
            val request =
                RegisterOwnerRequest(
                    username = "잘못된이메일",
                    email = "invalid-email-format", // 잘못된 형식
                    password = "password123!",
                    phoneNumber = "010-5555-5555",
                )

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/auth/owners/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.message").value("이메일 형식이 올바르지 않습니다."))
        }
    }
}
