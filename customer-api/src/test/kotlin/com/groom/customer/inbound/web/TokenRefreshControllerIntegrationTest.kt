package com.groom.customer.inbound.web

import com.auth0.jwt.JWT
import com.fasterxml.jackson.databind.ObjectMapper
import com.groom.customer.application.dto.LoginCommand
import com.groom.customer.application.dto.RegisterCustomerCommand
import com.groom.customer.application.dto.RegisterOwnerCommand
import com.groom.customer.application.service.CustomerAuthenticationService
import com.groom.customer.application.service.OwnerAuthenticationService
import com.groom.customer.application.service.RegisterCustomerService
import com.groom.customer.application.service.RegisterOwnerService
import com.groom.customer.common.TransactionApplier
import com.groom.customer.common.annotation.IntegrationTest
import com.groom.customer.inbound.web.dto.RefreshTokenRequest
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

@DisplayName("토큰 리프레시 컨트롤러 통합 테스트")
@IntegrationTest
@SpringBootTest
@AutoConfigureMockMvc
class TokenRefreshControllerIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var registerCustomerService: RegisterCustomerService

    @Autowired
    private lateinit var registerOwnerService: RegisterOwnerService

    @Autowired
    private lateinit var customerLoginService: CustomerAuthenticationService

    @Autowired
    private lateinit var ownerLoginService: OwnerAuthenticationService

    @Autowired
    private lateinit var userRepository: UserRepositoryImpl

//    @Autowired
//    private lateinit var storeRepository: StoreRepositoryImpl

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
//                    storeRepository.findByOwnerUserId(user.id).ifPresent { store ->
//                        storeRepository.delete(store)
//                    }
                    userRepository.delete(user)
                }
            }
        }
    }

    private fun trackEmail(email: String) {
        createdEmails.add(email)
    }

    @Nested
    @DisplayName("Refresh Token API 테스트")
    inner class RefreshTokenTests {
        @Test
        @DisplayName("POST /api/v1/auth/refresh - 유효한 Refresh Token으로 요청 시 200 OK와 새로운 Access Token을 반환한다")
        fun testSuccessfulRefresh() {
            // given
            val registerCommand =
                RegisterCustomerCommand(
                    username = "토큰갱신테스트",
                    email = "refreshtoken@example.com",
                    rawPassword = "password123!",
                    defaultAddress = "서울시 송파구",
                    defaultPhoneNumber = "010-5555-6666",
                )
            trackEmail(registerCommand.email)

            registerCustomerService.register(registerCommand)
            val loginCommand =
                LoginCommand(
                    email = "refreshtoken@example.com",
                    password = "password123!",
                    clientIp = "127.0.0.1",
                )
            val loginResult = customerLoginService.login(loginCommand)
            val refreshTokenRequest = RefreshTokenRequest(refreshToken = loginResult.refreshToken)

            // when & then
            val response =
                mockMvc
                    .perform(
                        post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer ${loginResult.accessToken}")
                            .content(objectMapper.writeValueAsString(refreshTokenRequest)),
                    ).andExpect(status().isOk)
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.accessToken").isString)
                    .andExpect(jsonPath("$.expiresIn").value(300))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andReturn()

            // 응답 데이터 추가 검증: 새로 발급된 Access Token 검증
            val responseBody = objectMapper.readTree(response.response.contentAsString)
            val newAccessToken = responseBody.get("accessToken").asText()

            // 새 Access Token의 JWT 구조 및 클레임 검증
            val decodedNewAccessToken = JWT.decode(newAccessToken)
            assert(decodedNewAccessToken.subject != null) { "New access token must have subject" }
            assert(decodedNewAccessToken.getClaim("role").asString() == "CUSTOMER") { "Role must be CUSTOMER" }
            assert(decodedNewAccessToken.issuer == jwtProperties.issuer) { "Issuer must match" }

            // 새 Access Token이 기존 Access Token과 다른지 확인
            val oldAccessToken = loginResult.accessToken
            assert(newAccessToken != oldAccessToken) { "New access token must be different from old one" }
        }

        @Test
        @DisplayName("POST /api/v1/auth/refresh - 존재하지 않는 Refresh Token으로 요청 시 401 Unauthorized를 반환한다")
        fun testRefreshWithInvalidToken() {
            // given
            // given
            val registerCommand =
                RegisterCustomerCommand(
                    username = "토큰갱신테스트",
                    email = "refreshtoken@example.com",
                    rawPassword = "password123!",
                    defaultAddress = "서울시 송파구",
                    defaultPhoneNumber = "010-5555-6666",
                )
            trackEmail(registerCommand.email)

            registerCustomerService.register(registerCommand)
            val loginCommand =
                LoginCommand(
                    email = "refreshtoken@example.com",
                    password = "password123!",
                    clientIp = "127.0.0.1",
                )
            val loginResult = customerLoginService.login(loginCommand)
            val invalidToken = "invalid-refresh-token"
            val refreshTokenRequest = RefreshTokenRequest(refreshToken = invalidToken)

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer ${loginResult.accessToken}")
                        .content(objectMapper.writeValueAsString(refreshTokenRequest)),
                ).andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("리프레시 토큰을 찾을 수 없습니다"))
        }

        @Test
        @DisplayName("POST /api/v1/auth/refresh - 재로그인 시 이전 Refresh Token이 무효화되어 갱신에 실패한다")
        fun testPreviousRefreshTokenInvalidatedAfterReLogin() {
            // given: 사용자 등록
            val registerCommand =
                RegisterCustomerCommand(
                    username = "재로그인테스트",
                    email = "relogin@example.com",
                    rawPassword = "password123!",
                    defaultAddress = "서울시 강남구",
                    defaultPhoneNumber = "010-9999-8888",
                )
            trackEmail(registerCommand.email)

            registerCustomerService.register(registerCommand)

            val loginCommand =
                LoginCommand(
                    email = "relogin@example.com",
                    password = "password123!",
                    clientIp = "192.168.1.100",
                )

            // 첫 번째 로그인
            val firstLoginResult = customerLoginService.login(loginCommand)
            val firstRefreshToken = firstLoginResult.refreshToken

            // 두 번째 로그인 (같은 사용자)
            val secondLoginResult = customerLoginService.login(loginCommand)
            val secondRefreshToken = secondLoginResult.refreshToken

            // 두 토큰이 서로 다른지 확인
            assert(firstRefreshToken != secondRefreshToken) { "First and second refresh tokens must be different" }

            // when & then: 첫 번째 Refresh Token으로 갱신 시도 → 실패
            val firstTokenRequest = RefreshTokenRequest(refreshToken = firstRefreshToken)
            mockMvc
                .perform(
                    post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer ${firstLoginResult.accessToken}")
                        .content(objectMapper.writeValueAsString(firstTokenRequest)),
                ).andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("리프레시 토큰을 찾을 수 없습니다"))

            // when & then: 두 번째 Refresh Token으로 갱신 시도 → 성공
            val secondTokenRequest = RefreshTokenRequest(refreshToken = secondRefreshToken)
            mockMvc
                .perform(
                    post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer ${secondLoginResult.accessToken}")
                        .content(objectMapper.writeValueAsString(secondTokenRequest)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.expiresIn").value(300))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
        }

        @Test
        @DisplayName("POST /api/v1/auth/refresh - 판매자 재로그인 시 이전 Refresh Token이 무효화되어 갱신에 실패한다")
        fun testOwnerPreviousRefreshTokenInvalidatedAfterReLogin() {
            // given: 판매자 등록
            val registerCommand =
                RegisterOwnerCommand(
                    username = "판매자재로그인테스트",
                    email = "ownerrelogin@example.com",
                    rawPassword = "password123!",
                    phoneNumber = "010-7777-6666",
                    storeName = "재로그인 테스트 스토어",
                    storeDescription = "재로그인 테스트 스토어 설명",
                )
            trackEmail(registerCommand.email)

            registerOwnerService.register(registerCommand)
            val loginCommand =
                LoginCommand(
                    email = "ownerrelogin@example.com",
                    password = "password123!",
                    clientIp = "192.168.1.200",
                )

            // 첫 번째 로그인
            val firstLoginResult = ownerLoginService.login(loginCommand)
            val firstRefreshToken = firstLoginResult.refreshToken

            // 두 번째 로그인 (같은 판매자)
            val secondLoginResult = ownerLoginService.login(loginCommand)
            val secondRefreshToken = secondLoginResult.refreshToken

            // 두 토큰이 서로 다른지 확인
            assert(firstRefreshToken != secondRefreshToken) { "First and second refresh tokens must be different" }

            // when & then: 첫 번째 Refresh Token으로 갱신 시도 → 실패
            val firstTokenRequest = RefreshTokenRequest(refreshToken = firstRefreshToken)
            mockMvc
                .perform(
                    post("/api/v1/auth/refresh")
                        .header("Authorization", "Bearer ${firstLoginResult.accessToken}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstTokenRequest)),
                ).andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("리프레시 토큰을 찾을 수 없습니다"))

            // when & then: 두 번째 Refresh Token으로 갱신 시도 → 성공
            val secondTokenRequest = RefreshTokenRequest(refreshToken = secondRefreshToken)
            mockMvc
                .perform(
                    post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer ${secondLoginResult.accessToken}")
                        .content(objectMapper.writeValueAsString(secondTokenRequest)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.expiresIn").value(300))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
        }
    }
}
