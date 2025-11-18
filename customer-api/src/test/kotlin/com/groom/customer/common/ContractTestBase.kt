package com.groom.customer.common

import com.groom.customer.adapter.inbound.web.InternalUserController
import com.groom.customer.application.service.InternalUserService
import io.restassured.module.mockmvc.RestAssuredMockMvc
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc

/**
 * Spring Cloud Contract Test를 위한 Base 클래스
 *
 * Contract 파일(YAML)을 기반으로 자동 생성된 테스트가 이 클래스를 상속받습니다.
 * - Provider 측(customer-service)에서 Contract를 검증
 * - MockMvc를 사용하여 경량 테스트 수행
 * - Service 레이어는 Mock으로 대체
 */
@WebMvcTest(InternalUserController::class)
abstract class ContractTestBase : IntegrationTestBase() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var internalUserService: InternalUserService

    @BeforeEach
    fun setup() {
        // RestAssured MockMvc 설정
        RestAssuredMockMvc.mockMvc(mockMvc)

        // Contract 파일의 테스트 데이터에 맞는 Mock 설정
        setupMocks()
    }

    private fun setupMocks() {
        // TODO: Contract 테스트를 위한 Mock 설정
        // 실제 서비스 로직 대신 Contract에 정의된 응답을 반환하도록 설정

        // 예시:
        // val userId = UUID.fromString("750e8400-e29b-41d4-a716-446655440001")
        // val userDto = UserInternalDto(...)
        // Mockito.`when`(internalUserService.getUserById(userId)).thenReturn(userDto)
    }
}
