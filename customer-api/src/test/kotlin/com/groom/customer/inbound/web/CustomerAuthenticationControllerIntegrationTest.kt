package com.groom.customer.inbound.web

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.databind.ObjectMapper
import com.groom.customer.application.dto.RegisterCustomerCommand
import com.groom.customer.application.service.CustomerAuthenticationService
import com.groom.customer.application.service.RegisterCustomerService
import com.groom.customer.common.TransactionApplier
import com.groom.customer.common.annotation.IntegrationTest
import com.groom.customer.inbound.web.dto.LoginRequest
import com.groom.customer.inbound.web.dto.SignupCustomerRequest
import com.groom.customer.outbound.repository.RefreshTokenRepositoryImpl
import com.groom.customer.outbound.repository.UserRepositoryImpl
import com.groom.customer.security.jwt.JwtProperties
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.Date
import java.util.UUID

@DisplayName("일반 고객 인증 컨트롤러 통합 테스트")
@IntegrationTest
@SpringBootTest
@AutoConfigureMockMvc
class CustomerAuthenticationControllerIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var registerCustomerService: RegisterCustomerService

    @Autowired
    private lateinit var customerLoginService: CustomerAuthenticationService

    @Autowired
    private lateinit var userRepository: UserRepositoryImpl

    @Autowired
    private lateinit var refreshTokenRepository: RefreshTokenRepositoryImpl

    @Autowired
    private lateinit var transactionApplier: TransactionApplier

    @Autowired
    private lateinit var jwtProperties: JwtProperties

    private val createdEmails = mutableListOf<String>()

    @BeforeEach
    fun setUp() {
        createdEmails.clear()
    }

    @AfterEach
    fun tearDown() {
        transactionApplier.applyPrimaryTransaction {
            createdEmails.forEach { email ->
                userRepository.findByEmail(email).ifPresent { user ->
                    refreshTokenRepository.findByUserId(user.id).ifPresent { token ->
                        refreshTokenRepository.delete(token)
                    }
                    userRepository.delete(user)
                }
            }
        }
    }

    private fun trackEmail(email: String) {
        createdEmails.add(email)
    }

    @Nested
    @DisplayName("일반 고객 로그인 API 테스트")
    inner class CustomerLoginTests {
        @Test
        @DisplayName("POST /api/v1/auth/customers/login - 정상적인 로그인 요청 시 200 OK와 토큰 정보를 반환한다")
        fun testSuccessfulLogin() {
            // given
            val registerCommand =
                RegisterCustomerCommand(
                    username = "고객테스트",
                    email = "customer@example.com",
                    rawPassword = "password123!",
                    defaultAddress = "서울시 강남구",
                    defaultPhoneNumber = "010-1111-2222",
                )
            trackEmail(registerCommand.email)

            transactionApplier.applyPrimaryTransaction {
                registerCustomerService.register(registerCommand)
            }

            val loginRequest =
                LoginRequest(
                    email = "customer@example.com",
                    password = "password123!",
                )

            // when & then
            val response =
                mockMvc
                    .perform(
                        post("/api/v1/auth/customers/login")
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
            assert(decodedAccessToken.getClaim("role").asString() == "CUSTOMER") { "Role must be CUSTOMER" }
            assert(decodedAccessToken.issuer == jwtProperties.issuer) { "Issuer must match" }

            // Refresh Token 검증
            val decodedRefreshToken = JWT.decode(refreshToken)
            assert(decodedRefreshToken.subject != null) { "Refresh token must have subject" }
            assert(decodedRefreshToken.getClaim("role").asString() == "CUSTOMER") { "Role must be CUSTOMER" }
            assert(decodedRefreshToken.issuer == jwtProperties.issuer) { "Issuer must match" }
        }

        @Test
        @DisplayName("POST /api/v1/auth/customers/login - 잘못된 비밀번호로 로그인 시도 시 401 Unauthorized를 반환한다")
        fun testLoginWithWrongPassword() {
            // given
            val registerCommand =
                RegisterCustomerCommand(
                    username = "비밀번호오류",
                    email = "wrongpwd@example.com",
                    rawPassword = "correctPassword123!",
                    defaultAddress = "서울시 서초구",
                    defaultPhoneNumber = "010-3333-4444",
                )
            trackEmail(registerCommand.email)

            transactionApplier.applyPrimaryTransaction {
                registerCustomerService.register(registerCommand)
            }

            val loginRequest =
                LoginRequest(
                    email = "wrongpwd@example.com",
                    password = "wrongPassword",
                )

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/auth/customers/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)),
                ).andExpect(status().isUnauthorized)
        }

        @Test
        @DisplayName("POST /api/v1/auth/customers/login - 존재하지 않는 이메일로 로그인 시도 시 401 Unauthorized를 반환한다")
        fun testLoginWithNonExistentEmail() {
            // given
            val loginRequest =
                LoginRequest(
                    email = "notexist@example.com",
                    password = "password123",
                )

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/auth/customers/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)),
                ).andExpect(status().isUnauthorized)
        }
    }

    @Nested
    @DisplayName("일반 고객 로그아웃 API 테스트")
    inner class CustomerLogoutTests {
        @Test
        @DisplayName("POST /api/v1/auth/customers/logout - 로그인한 사용자가 로그아웃하면 204 No Content를 반환한다")
        fun testSuccessfulLogout() {
            // given
            val registerCommand =
                RegisterCustomerCommand(
                    username = "로그아웃테스트",
                    email = "logoutcustomer@example.com",
                    rawPassword = "password123!",
                    defaultAddress = "서울시 강남구",
                    defaultPhoneNumber = "010-9999-0000",
                )
            trackEmail(registerCommand.email)

            transactionApplier.applyPrimaryTransaction {
                registerCustomerService.register(registerCommand)
            }

            // 로그인하여 access token 획득
            val loginRequest =
                LoginRequest(
                    email = "logoutcustomer@example.com",
                    password = "password123!",
                )

            val loginResponse =
                mockMvc
                    .perform(
                        post("/api/v1/auth/customers/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)),
                    ).andExpect(status().isOk)
                    .andReturn()
                    .response
                    .contentAsString

            val loginResponseBody = objectMapper.readTree(loginResponse)
            val accessToken = loginResponseBody.get("accessToken").asText()
            val refreshToken = loginResponseBody.get("refreshToken").asText()

            // JWT 토큰에서 userId 추출 (실제 환경에서는 Istio가 처리)
            val decodedJWT = JWT.decode(accessToken)
            val userId = decodedJWT.subject

            // when & then
            // Istio가 JWT를 검증하고 X-User-Id 헤더를 주입한다고 가정
            mockMvc
                .perform(
                    post("/api/v1/auth/customers/logout")
                        .header("X-User-Id", userId),
                ).andExpect(status().isNoContent)
        }

        // JWT 검증 관련 테스트는 제거됨:
        // - Istio API Gateway가 JWT 검증을 담당
        // - 만료된 토큰, 잘못된 서명, 잘못된 issuer, 토큰 없음 등은 Istio에서 처리
        // - 이 서비스는 Istio가 검증한 X-User-Id 헤더만 사용

    }

    @Nested
    @DisplayName("일반 고객 회원가입 API 테스트")
    inner class CustomerSignupTests {
        @Test
        @DisplayName("POST /api/v1/auth/customers/signup - 정상적인 회원가입 요청 시 201 Created와 회원 정보를 반환한다")
        fun testSuccessfulSignup() {
            // given
            val request =
                SignupCustomerRequest(
                    username = "홍길동",
                    email = "hong@example.com",
                    password = "P@ssw0rd!",
                    defaultAddress = "서울특별시 송파구 올림픽로 300",
                    defaultPhoneNumber = "010-1234-5678",
                )
            trackEmail(request.email)

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/auth/customers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.username").value("홍길동"))
                .andExpect(jsonPath("$.email").value("hong@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.createdAt").exists())
        }

        @Test
        @DisplayName("POST /api/v1/auth/customers/signup - 동일한 이메일로 중복 가입 시도 시 409 Conflict를 반환한다")
        fun testDuplicateEmailSignup() {
            // given
            val firstRequest =
                SignupCustomerRequest(
                    username = "박민수",
                    email = "park@example.com",
                    password = "password123!",
                    defaultAddress = "부산시",
                    defaultPhoneNumber = "010-3333-3333",
                )
            trackEmail(firstRequest.email)

            // 첫 번째 가입
            mockMvc
                .perform(
                    post("/api/v1/auth/customers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)),
                ).andExpect(status().isCreated)

            // given - 동일 이메일로 다시 가입 시도
            val duplicateRequest =
                SignupCustomerRequest(
                    username = "최다은",
                    email = "park@example.com", // 동일 이메일
                    password = "different123!",
                    defaultAddress = "인천시",
                    defaultPhoneNumber = "010-4444-4444",
                )

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/auth/customers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)),
                ).andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"))
                .andExpect(jsonPath("$.message").value("이미 존재하는 이메일입니다: park@example.com"))
        }

        @Test
        @DisplayName("POST /api/v1/auth/customers/signup - 잘못된 이메일 형식으로 가입 시도 시 400 Bad Request를 반환한다")
        fun testInvalidEmailFormat() {
            // given
            val request =
                SignupCustomerRequest(
                    username = "정수진",
                    email = "invalid-email-format", // 잘못된 형식
                    password = "password123!",
                    defaultAddress = "대구시",
                    defaultPhoneNumber = "010-5555-5555",
                )

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/auth/customers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.message").value("이메일 형식이 올바르지 않습니다."))
        }

        @Test
        @DisplayName("POST /api/v1/auth/customers/signup - 회원가입 성공 시 DB에 User와 UserProfile이 모두 저장된다")
        fun testSignupSavesUserAndProfile() {
            // given
            val request =
                SignupCustomerRequest(
                    username = "송지효",
                    email = "song@example.com",
                    password = "password123!",
                    defaultAddress = "서울시 마포구",
                    defaultPhoneNumber = "010-1010-1010",
                )
            trackEmail(request.email)

            // when
            mockMvc
                .perform(
                    post("/api/v1/auth/customers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isCreated)

            // then - DB에 저장되었는지 확인
            val savedUser = userRepository.findByEmail("song@example.com").orElseThrow()
            assert(savedUser.username == "송지효")
            assert(savedUser.profile != null)
            assert(savedUser.profile!!.fullName == "송지효")
            assert(savedUser.profile!!.phoneNumber == "010-1010-1010")
            assert(savedUser.profile!!.defaultAddress == "서울시 마포구")
        }
    }
}
