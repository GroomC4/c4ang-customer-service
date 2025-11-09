package com.groom.customer.common

import com.groom.infra.testcontainers.K8sContainerExtension
import com.groom.infra.testcontainers.K8sHelmHelper
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait

/**
 * 모든 통합 테스트에서 공유하는 컨테이너 싱글톤
 *
 * 이 객체는 JVM이 살아있는 동안 컨테이너를 재사용하여
 * 테스트 실행 속도를 크게 향상시킵니다.
 *
 * 사용 방법:
 * 1. AbstractIntegrationTest를 상속받은 테스트 클래스 작성
 * 2. 컨테이너 설정은 자동으로 적용됨
 *
 * 참고:
 * - 컨테이너 재사용을 위해 ~/.testcontainers.properties에
 *   testcontainers.reuse.enable=true 설정 필요
 */
object SharedTestContainers {
    /**
     * 공유 PostgreSQL 컨테이너
     * 첫 테스트 실행 시 한 번만 시작되고 이후 재사용됨
     */
    val postgres: PostgreSQLContainer<*> by lazy {
        PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("customer_db")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true) // 컨테이너 재사용 활성화
            .apply {
                start()
                println("✅ Shared PostgreSQL container started: $host:$firstMappedPort")
            }
    }

    /**
     * 공유 Redis 컨테이너
     * 첫 테스트 실행 시 한 번만 시작되고 이후 재사용됨
     */
    val redis: GenericContainer<*> by lazy {
        GenericContainer("redis:7-alpine")
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort())
            .withReuse(true) // 컨테이너 재사용 활성화
            .apply {
                start()
                println("✅ Shared Redis container started: $host:${getMappedPort(6379)}")
            }
    }

    private var k8sInitialized = false

    /**
     * K8s 리소스 초기화 (Helm 차트 배포 등)
     * 모든 테스트에서 한 번만 실행됨
     *
     * @Synchronized를 사용하여 동시 실행 방지
     */
    @Synchronized
    fun initializeK8sResources() {
        if (k8sInitialized) {
            println("⏭️  K8s resources already initialized, skipping...")
            return
        }

        try {
            val client = K8sContainerExtension.getKubernetesClient()

            // 네임스페이스 생성
            K8sHelmHelper.createNamespace("customer-test")
            println("✅ K8s test namespace created: customer-test")

            // Helm 차트 배포 (호스트에서 K3s로 배포)
            val success =
                K8sHelmHelper.installHelmChart(
                    chartPath = "../c4ang-infra/helm/test-infrastructure",
                    releaseName = "test-infra",
                    namespace = "customer-test",
                    values =
                        mapOf(
                            "postgresql.auth.database" to "customer_db",
                            "postgresql.auth.username" to "test",
                            "postgresql.auth.password" to "test",
                        ),
                )

            require(success) { "Failed to install test infrastructure" }
            k8sInitialized = true
            println("✅ Test infrastructure installed successfully")
        } catch (e: Exception) {
            println("❌ Failed to initialize K8s resources: ${e.message}")
            throw e
        }
    }
}
