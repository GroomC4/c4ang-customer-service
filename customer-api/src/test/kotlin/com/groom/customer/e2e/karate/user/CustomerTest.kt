package com.groom.customer.e2e.karate.user

import com.groom.customer.common.annotation.E2EScenarioTest
import com.intuit.karate.junit5.Karate
import org.junit.jupiter.api.DisplayName
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort

@E2EScenarioTest
@DisplayName("일바고객 인증 플로우 전체 시나리오 테스트")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CustomerTest {
    @LocalServerPort
    private var port: Int = 0

    @Karate.Test
    @DisplayName("회원가입 → 로그인 → 토큰 리프레시 → 로그아웃")
    fun `customer signup to logout`(): Karate {
        // Spring Boot 서버의 실제 포트를 Karate에 전달
        System.setProperty("karate.port", port.toString())
        // classpath:karate 경로의 모든 .feature 파일 실행
        return Karate.run("classpath:karate/customer-authentication.feature")
    }
}
