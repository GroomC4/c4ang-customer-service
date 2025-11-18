package com.groom.customer.adapter.inbound.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.groom.customer.adapter.inbound.web.dto.RegisterOwnerRequest
import com.groom.customer.adapter.inbound.web.dto.SignupCustomerRequest
import com.groom.customer.adapter.outbound.persistence.UserRepository
import org.springframework.boot.test.context.SpringBootTest
import com.groom.customer.domain.model.UserRole
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(properties = ["spring.profiles.active=test"])
@AutoConfigureMockMvc
class SignupIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    // 각 테스트에서 생성한 이메일 목록을 추적
    private val createdEmails = mutableListOf<String>()

    @BeforeEach
    fun setUp() {
        createdEmails.clear()
    }

    @AfterEach
    fun tearDown() {
        // 이 테스트에서 생성한 사용자만 삭제
        // 동일한 이메일로 CUSTOMER와 OWNER가 모두 생성될 수 있으므로 두 역할 모두 확인
        createdEmails.forEach { email ->
            // CUSTOMER 삭제
            userRepository.findByEmailAndRole(email, UserRole.CUSTOMER).ifPresent { user ->
                // UserProfile은 cascade로 자동 삭제됨
                userRepository.delete(user)
            }
            // OWNER 삭제
            userRepository.findByEmailAndRole(email, UserRole.OWNER).ifPresent { user ->
                // 판매자인 경우 스토어도 함께 삭제
//                user.id.let { id ->
//                    storeRepository.findByOwnerUserId(id).ifPresent { store ->
//                        // StoreRating은 cascade로 자동 삭제됨
//                        storeRepository.delete(store)
//                    }
//                }
                // UserProfile은 cascade로 자동 삭제됨
                userRepository.delete(user)
            }
        }
    }

    // 테스트에서 사용자 생성 시 이메일을 추적하는 헬퍼 함수
    private fun trackEmail(email: String) {
        createdEmails.add(email)
    }

    // ========== CUSTOMER와 OWNER 역할 분리 테스트 ==========

    @Test
    fun `동일한 이메일로 CUSTOMER와 OWNER 각각 가입 시 모두 성공한다`() {
        // given - 먼저 CUSTOMER로 가입
        val customerRequest =
            SignupCustomerRequest(
                username = "일반회원",
                email = "shared@example.com",
                password = "password123!",
                defaultAddress = "서울시",
                defaultPhoneNumber = "010-1111-1111",
            )
        trackEmail(customerRequest.email)

        // when - CUSTOMER 가입
        mockMvc
            .perform(
                post("/api/v1/auth/customers/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(customerRequest)),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.role").value("CUSTOMER"))

        // given - 동일한 이메일로 OWNER 가입 시도
        val ownerRequest =
            RegisterOwnerRequest(
                username = "판매회원",
                email = "shared@example.com", // 동일 이메일
                password = "password123!",
                phoneNumber = "010-2222-2222",
            )

        // when & then - OWNER 가입도 성공해야 함
        mockMvc
            .perform(
                post("/api/v1/auth/owners/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(ownerRequest)),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.user.email").value("shared@example.com"))

        // DB에서 두 사용자 모두 조회 가능한지 확인
        val customerUser = userRepository.findByEmailAndRole("shared@example.com", UserRole.CUSTOMER).orElseThrow()
        val ownerUser = userRepository.findByEmailAndRole("shared@example.com", UserRole.OWNER).orElseThrow()

        assert(customerUser.username == "일반회원")
        assert(customerUser.role.name == "CUSTOMER")
        assert(ownerUser.username == "판매회원")
        assert(ownerUser.role.name == "OWNER")
    }
}
