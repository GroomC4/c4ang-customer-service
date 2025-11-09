package com.groom.customer.common

import com.groom.customer.common.annotation.IntegrationTest
import com.groom.customer.fixture.MockStoreFeignClientConfig
import org.junit.jupiter.api.BeforeAll
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

/**
 * 모든 통합 테스트의 Base 클래스
 *
 * 이 클래스를 상속받으면:
 * 1. 공유 컨테이너(PostgreSQL, Redis) 설정이 자동으로 적용됨
 * 2. K8s 클러스터 및 Helm 차트가 자동으로 배포됨
 * 3. Spring Boot 애플리케이션 설정이 자동으로 구성됨
 *
 * 사용 예시:
 * ```kotlin
 * @IntegrationTest
 * @AutoConfigureMockMvc
 * class MyIntegrationTest : AbstractIntegrationTest() {
 *
 *     @Autowired
 *     private lateinit var mockMvc: MockMvc
 *
 *     @Test
 *     fun `테스트`() {
 *         // 바로 테스트 로직 작성
 *         // 컨테이너와 K8s 리소스는 이미 준비됨
 *     }
 * }
 * ```
 *
 * 데이터 초기화/정리 예시:
 * ```kotlin
 * @IntegrationTest
 * class MyIntegrationTest : AbstractIntegrationTest() {
 *
 *     @Autowired
 *     private lateinit var userRepository: UserRepository
 *
 *     private val createdUserIds = mutableListOf<UUID>()
 *
 *     @AfterEach
 *     fun cleanup() {
 *         createdUserIds.forEach { userRepository.deleteById(it) }
 *         createdUserIds.clear()
 *     }
 *
 *     @Test
 *     fun `테스트`() {
 *         val user = userRepository.save(User(...))
 *         createdUserIds.add(user.id)
 *         // 테스트 로직
 *     }
 * }
 * ```
 *
 * @Sql 사용 예시:
 * ```kotlin
 * @IntegrationTest
 * class MyIntegrationTest : AbstractIntegrationTest() {
 *
 *     @Test
 *     @Sql(
 *         scripts = ["/sql/insert-test-data.sql"],
 *         executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
 *     )
 *     @Sql(
 *         scripts = ["/sql/cleanup.sql"],
 *         executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
 *     )
 *     fun `테스트`() {
 *         // 테스트 로직
 *     }
 * }
 * ```
 *
 * 참고:
 * - 컨테이너가 재사용되므로 테스트 후 반드시 데이터를 정리해야 합니다
 * - @AfterEach 또는 @Sql을 사용하여 cleanup 로직을 구현하세요
 */
@IntegrationTest
@Import(MockStoreFeignClientConfig::class)
abstract class AbstractIntegrationTest {
    companion object {
        /**
         * Spring Boot 애플리케이션 설정을 동적으로 구성
         *
         * 이 메서드는 모든 하위 테스트 클래스에 자동으로 적용됩니다.
         * SharedTestContainers에서 시작된 컨테이너 정보를
         * Spring Boot 설정에 주입합니다.
         */
        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            val postgres = SharedTestContainers.postgres
            val redis = SharedTestContainers.redis

            // PostgreSQL 설정 - master
            registry.add("spring.datasource.master.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.master.username") { postgres.username }
            registry.add("spring.datasource.master.password") { postgres.password }
            registry.add("spring.datasource.master.driver-class-name") { "org.postgresql.Driver" }

            // PostgreSQL 설정 - replica (동일한 컨테이너 사용)
            registry.add("spring.datasource.replica.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.replica.username") { postgres.username }
            registry.add("spring.datasource.replica.password") { postgres.password }
            registry.add("spring.datasource.replica.driver-class-name") { "org.postgresql.Driver" }

            // Redis 설정 - Spring Data Redis
            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }

            // Redis 설정 - Redisson
            registry.add("spring.redis.host") { redis.host }
            registry.add("spring.redis.port") { redis.getMappedPort(6379) }
        }

        /**
         * K8s 리소스 초기화
         *
         * 이 메서드는 모든 하위 테스트 클래스에서 한 번만 실행됩니다.
         * K8s 클러스터에 네임스페이스를 생성하고 Helm 차트를 배포합니다.
         */
        @BeforeAll
        @JvmStatic
        fun setupK8sResources() {
            SharedTestContainers.initializeK8sResources()
        }
    }
}
