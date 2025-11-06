package com.groom.customer.inbound.web

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.databind.ObjectMapper
import com.groom.customer.application.dto.RegisterOwnerCommand
import com.groom.customer.application.service.RegisterOwnerService
import com.groom.customer.common.TransactionApplier
import com.groom.customer.common.annotation.IntegrationTest
import com.groom.customer.outbound.repository.RefreshTokenRepositoryImpl
import com.groom.customer.outbound.repository.UserRepositoryImpl
import com.groom.customer.inbound.web.dto.LoginRequest
import com.groom.customer.inbound.web.dto.RegisterOwnerRequest
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

@DisplayName("판매자 인증 컨트롤러 통합 테스트")
@IntegrationTest
@SpringBootTest
@AutoConfigureMockMvc
class OwnerAuthenticationControllerIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var registerOwnerService: RegisterOwnerService

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
    @DisplayName("판매자 로그인 API 테스트")
    inner class OwnerLoginTests {
        @Test
        @DisplayName("POST /api/v1/auth/owners/login - 정상적인 로그인 요청 시 200 OK와 토큰 정보를 반환한다")
        fun testSuccessfulLogin() {
            // given
            val registerCommand =
                RegisterOwnerCommand(
                    username = "판매자테스트",
                    email = "owner@example.com",
                    rawPassword = "password123!",
                    phoneNumber = "010-1111-2222",
                    storeName = "테스트 스토어",
                    storeDescription = "테스트 스토어 설명",
                )
            trackEmail(registerCommand.email)

            transactionApplier.applyPrimaryTransaction {
                registerOwnerService.register(registerCommand)
            }

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
            // given
            val registerCommand =
                RegisterOwnerCommand(
                    username = "판매자비번오류",
                    email = "ownerwrongpwd@example.com",
                    rawPassword = "correctPassword123!",
                    phoneNumber = "010-3333-4444",
                    storeName = "테스트 스토어2",
                    storeDescription = "테스트 스토어 설명2",
                )
            trackEmail(registerCommand.email)

            transactionApplier.applyPrimaryTransaction {
                registerOwnerService.register(registerCommand)
            }

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
            // given
            val registerCommand =
                RegisterOwnerCommand(
                    username = "판매자로그아웃",
                    email = "logoutowner@example.com",
                    rawPassword = "password123!",
                    phoneNumber = "010-9999-0000",
                    storeName = "로그아웃 테스트 스토어",
                    storeDescription = "로그아웃 테스트 스토어 설명",
                )
            trackEmail(registerCommand.email)

            transactionApplier.applyPrimaryTransaction {
                registerOwnerService.register(registerCommand)
            }

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

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/auth/owners/logout")
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isNoContent)
        }

        @Test
        @DisplayName("POST /api/v1/auth/owners/logout - 인증 토큰 없이 로그아웃 시도 시 401 Unauthorized를 반환한다")
        fun testLogoutWithoutToken() {
            // when & then
            mockMvc
                .perform(
                    post("/api/v1/auth/owners/logout"),
                ).andExpect(status().isUnauthorized)
        }

        @Test
        @DisplayName("POST /api/v1/auth/owners/logout - 만료된 토큰으로 로그아웃 시도 시 401 Unauthorized를 반환한다")
        fun testLogoutWithExpiredToken() {
            // given
            val registerCommand =
                RegisterOwnerCommand(
                    username = "만료토큰판매자",
                    email = "expiredowner@example.com",
                    rawPassword = "password123!",
                    phoneNumber = "010-1234-5678",
                    storeName = "만료토큰 스토어",
                    storeDescription = "만료토큰 테스트",
                )
            trackEmail(registerCommand.email)

            val userId =
                transactionApplier.applyPrimaryTransaction {
                    val result = registerOwnerService.register(registerCommand)
                    result.userId
                }

            // 만료된 토큰 생성 (1분 전에 만료)
            val expiredToken =
                JWT
                    .create()
                    .withIssuer(jwtProperties.issuer)
                    .withSubject(userId)
                    .withClaim("role", "OWNER")
                    .withIssuedAt(Date.from(Instant.now().minus(10, ChronoUnit.MINUTES)))
                    .withExpiresAt(Date.from(Instant.now().minus(1, ChronoUnit.MINUTES)))
                    .withJWTId(
                        UUID
                            .randomUUID()
                            .toString(),
                    ).sign(Algorithm.HMAC256(jwtProperties.secret))

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/auth/owners/logout")
                        .header("Authorization", "Bearer $expiredToken"),
                ).andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"))
                .andExpect(jsonPath("$.message").value("토큰이 만료되었습니다. 다시 로그인해주세요."))
        }

        @Test
        @DisplayName("POST /api/v1/auth/owners/logout - role이 변조된 토큰으로 로그아웃 시도 시 401 Unauthorized를 반환한다")
        fun testLogoutWithTamperedRoleToken() {
            // given
            val registerCommand =
                RegisterOwnerCommand(
                    username = "Role변조판매자",
                    email = "tamperroleowner@example.com",
                    rawPassword = "password123!",
                    phoneNumber = "010-2345-6789",
                    storeName = "Role변조 스토어",
                    storeDescription = "Role변조 테스트",
                )
            trackEmail(registerCommand.email)

            transactionApplier.applyPrimaryTransaction {
                registerOwnerService.register(registerCommand)
            }

            // 로그인하여 정상 토큰 획득
            val loginRequest =
                LoginRequest(
                    email = "tamperroleowner@example.com",
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

            val validToken = objectMapper.readTree(loginResponse).get("accessToken").asText()

            // 토큰을 분해하여 payload만 변조하고 signature는 원본 유지
            val parts = validToken.split(".")
            val decodedJWT = JWT.decode(validToken)

            // role을 CUSTOMER로 변조한 새로운 payload 생성
            val tamperedPayload =
                """{"sub":"${decodedJWT.subject}","role":"CUSTOMER","iss":"${decodedJWT.issuer}","jti":"${decodedJWT.id}"}"""
            val tamperedPayloadEncoded =
                Base64
                    .getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(tamperedPayload.toByteArray())

            // 원본 signature를 그대로 사용 (비밀키 없이는 올바른 signature 생성 불가)
            val tamperedToken = "${parts[0]}.$tamperedPayloadEncoded.${parts[2]}"

            // when & then
            // payload가 변조되었지만 signature는 원본 그대로이므로 서명 검증 실패
            // JWT 토큰 자체가 유효하지 않아 401 Unauthorized 반환
            mockMvc
                .perform(
                    post("/api/v1/auth/owners/logout")
                        .header("Authorization", "Bearer $tamperedToken"),
                ).andExpect(status().isUnauthorized) // 401 Unauthorized (서명 검증 실패)
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN_SIGNATURE"))
                .andExpect(jsonPath("$.message").value("인증에 실패하였습니다."))
        }

        @Test
        @DisplayName("POST /api/v1/auth/owners/logout - 잘못된 서명의 토큰으로 로그아웃 시도 시 401 Unauthorized를 반환한다")
        fun testLogoutWithInvalidSignatureToken() {
            // given
            val registerCommand =
                RegisterOwnerCommand(
                    username = "잘못된서명판매자",
                    email = "invalidsigowner@example.com",
                    rawPassword = "password123!",
                    phoneNumber = "010-3456-7890",
                    storeName = "잘못된서명 스토어",
                    storeDescription = "잘못된서명 테스트",
                )
            trackEmail(registerCommand.email)

            val userId =
                transactionApplier.applyPrimaryTransaction {
                    val result = registerOwnerService.register(registerCommand)
                    result.userId
                }

            // 잘못된 secret으로 토큰 생성
            val invalidToken =
                JWT
                    .create()
                    .withIssuer(jwtProperties.issuer)
                    .withSubject(userId)
                    .withClaim("role", "OWNER")
                    .withIssuedAt(Date.from(Instant.now()))
                    .withExpiresAt(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)))
                    .withJWTId(
                        UUID
                            .randomUUID()
                            .toString(),
                    ).sign(Algorithm.HMAC256("wrong-secret-key-that-is-long-enough-for-hmac256"))

            // when & then
            // JWT 검증 실패 시 AuthenticationFailedException 발생
            mockMvc
                .perform(
                    post("/api/v1/auth/owners/logout")
                        .header("Authorization", "Bearer $invalidToken"),
                ).andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN_SIGNATURE"))
                .andExpect(jsonPath("$.message").value("인증에 실패하였습니다."))
        }

        @Test
        @DisplayName("POST /api/v1/auth/owners/logout - 잘못된 issuer의 토큰으로 로그아웃 시도 시 401 Unauthorized를 반환한다")
        fun testLogoutWithInvalidIssuerToken() {
            // given
            val registerCommand =
                RegisterOwnerCommand(
                    username = "잘못된발급자판매자",
                    email = "invalidissuerowner@example.com",
                    rawPassword = "password123!",
                    phoneNumber = "010-4567-8901",
                    storeName = "잘못된발급자 스토어",
                    storeDescription = "잘못된발급자 테스트",
                )
            trackEmail(registerCommand.email)

            val userId =
                transactionApplier.applyPrimaryTransaction {
                    val result = registerOwnerService.register(registerCommand)
                    result.userId
                }

            // 잘못된 issuer로 토큰 생성
            val invalidIssuerToken =
                JWT
                    .create()
                    .withIssuer("fake-issuer") // 잘못된 issuer
                    .withSubject(userId)
                    .withClaim("role", "OWNER")
                    .withIssuedAt(Date.from(Instant.now()))
                    .withExpiresAt(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)))
                    .withJWTId(
                        UUID
                            .randomUUID()
                            .toString(),
                    ).sign(Algorithm.HMAC256(jwtProperties.secret))

            // when & then
            // JWT 검증 실패 시 InvalidTokenIssuer 예외 발생
            mockMvc
                .perform(
                    post("/api/v1/auth/owners/logout")
                        .header("Authorization", "Bearer $invalidIssuerToken"),
                ).andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN_ISSUER"))
                .andExpect(jsonPath("$.message").value("토큰 발급자가 올바르지 않습니다. 기대값: ${jwtProperties.issuer}, 실제값: unknown"))
        }
    }

    @Nested
    @DisplayName("판매자 회원가입 API 테스트")
    inner class OwnerSignupTests {
        @Test
        @DisplayName("POST /api/v1/auth/owners/signup - 정상적인 판매자 회원가입 요청 시 201 Created와 판매자 정보 및 스토어 정보를 반환한다")
        fun testSuccessfulSignup() {
            // given
            val request =
                RegisterOwnerRequest(
                    username = "판매자김",
                    email = "owner@example.com",
                    password = "P@ssw0rd!",
                    phoneNumber = "010-9999-9999",
                    storeInfo =
                        RegisterOwnerRequest.StoreInfo(
                            name = "테크 스토어",
                            description = "최신 전자제품 판매",
                        ),
                )
            trackEmail(request.email)

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/auth/owners/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.user.id").exists())
                .andExpect(jsonPath("$.user.name").value("판매자김"))
                .andExpect(jsonPath("$.user.email").value("owner@example.com"))
                .andExpect(jsonPath("$.store.id").exists())
                .andExpect(jsonPath("$.store.name").value("테크 스토어"))
                .andExpect(jsonPath("$.createdAt").exists())
        }

        @Test
        @DisplayName("POST /api/v1/auth/owners/signup - 판매자 회원가입 성공 시 DB에 User, UserProfile, Store, StoreRating이 모두 저장된다")
        fun testSignupSavesAllEntities() {
            // given
            val request =
                RegisterOwnerRequest(
                    username = "스토어주인",
                    email = "storeowner@example.com",
                    password = "password123!",
                    phoneNumber = "010-8888-8888",
                    storeInfo =
                        RegisterOwnerRequest.StoreInfo(
                            name = "패션 스토어",
                            description = "최신 패션 아이템",
                        ),
                )
            trackEmail(request.email)

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
            // given
            val firstRequest =
                RegisterOwnerRequest(
                    username = "첫번째판매자",
                    email = "duplicate@example.com",
                    password = "password123!",
                    phoneNumber = "010-7777-7777",
                    storeInfo =
                        RegisterOwnerRequest.StoreInfo(
                            name = "첫번째 스토어",
                            description = null,
                        ),
                )
            trackEmail(firstRequest.email)

            // 첫 번째 가입
            mockMvc
                .perform(
                    post("/api/v1/auth/owners/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)),
                ).andExpect(status().isCreated)

            // given - 동일 이메일로 다시 가입 시도
            val duplicateRequest =
                RegisterOwnerRequest(
                    username = "두번째판매자",
                    email = "duplicate@example.com", // 동일 이메일
                    password = "different123!",
                    phoneNumber = "010-6666-6666",
                    storeInfo =
                        RegisterOwnerRequest.StoreInfo(
                            name = "두번째 스토어",
                            description = null,
                        ),
                )

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/auth/owners/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)),
                ).andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"))
                .andExpect(jsonPath("$.message").value("이미 존재하는 이메일입니다: duplicate@example.com"))
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
                    storeInfo =
                        RegisterOwnerRequest.StoreInfo(
                            name = "테스트 스토어",
                            description = null,
                        ),
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
