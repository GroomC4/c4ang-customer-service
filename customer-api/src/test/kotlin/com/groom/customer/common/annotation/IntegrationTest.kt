package com.groom.customer.common.annotation

import com.groom.customer.common.extension.CustomerServiceContainerExtension
import com.groom.infra.testcontainers.IntegrationTest as BaseIntegrationTest
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest

/**
 * Customer Service 통합 테스트용 어노테이션
 *
 * c4ang-infra의 BaseIntegrationTest를 상속받아 Customer Service에 필요한
 * 컨테이너 Extension을 추가합니다.
 *
 * 사용 예시:
 * ```kotlin
 * @IntegrationTest
 * @AutoConfigureMockMvc
 * class CustomerControllerIntegrationTest {
 *     @Test
 *     fun `통합 테스트`() {
 *         // 테스트 로직
 *     }
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@BaseIntegrationTest
@SpringBootTest
@ExtendWith(CustomerServiceContainerExtension::class)
annotation class IntegrationTest
