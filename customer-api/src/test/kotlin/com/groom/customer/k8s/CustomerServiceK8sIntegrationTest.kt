package com.groom.customer.k8s

import com.groom.infra.testcontainers.K8sContainerExtension
import com.groom.infra.testcontainers.K8sHelmHelper
import com.groom.infra.testcontainers.K8sIntegrationTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

@K8sIntegrationTest
class CustomerServiceK8sIntegrationTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun setupK8sResources() {
            val client = K8sContainerExtension.getKubernetesClient()

            // 네임스페이스 생성
            K8sHelmHelper.createNamespace("customer-test")
            println("✅ K8s test namespace created: customer-test")

            // Helm 차트 배포 (호스트에서 K3s로 배포)
            // 주의: helm dependency build를 먼저 실행해야 합니다!
            //
            // cd c4ang-infra/helm
            // ./build-dependencies.sh
            //
            // 그 후 아래 주석을 해제하여 사용:

             val success = K8sHelmHelper.installHelmChart(
                 chartPath = "../c4ang-infra/helm/test-infrastructure",
                 releaseName = "test-infra",
                 namespace = "customer-test",
                 values = mapOf(
                     "postgresql.auth.database" to "customer_db",
                     "postgresql.auth.username" to "test",
                     "postgresql.auth.password" to "test"
                 )
             )

             require(success) { "Failed to install test infrastructure" }
             println("✅ Test infrastructure installed successfully")
        }
    }

    @Test
    fun `K8s 클러스터가 정상적으로 시작된다`() {
        // 테스트 로직
        val client = K8sContainerExtension.getKubernetesClient()

        // 네임스페이스가 생성되었는지 확인
        val namespaces = client.namespaces().list()
        val customerTestNamespace = namespaces.items.find { it.metadata.name == "customer-test" }

        assert(customerTestNamespace != null) {
            "customer-test namespace should exist"
        }

        println("✅ K8s cluster is running successfully")
    }

    @Test
    fun `K8s API 서버에 접근할 수 있다`() {
        val apiServerUrl = K8sContainerExtension.getApiServerUrl()

        assert(apiServerUrl.isNotEmpty()) {
            "API Server URL should not be empty"
        }

        println("✅ K8s API Server URL: $apiServerUrl")
    }
}
