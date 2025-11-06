package com.groom.customer.common.extension

import com.groom.infra.testcontainers.BaseContainerExtension
import java.io.File

/**
 * Customer Service용 통합 테스트 컨테이너 Extension
 *
 * c4ang-infra의 BaseContainerExtension을 상속받아 Customer Service에 필요한
 * Docker Compose 파일 경로를 제공합니다.
 */
class CustomerServiceContainerExtension : BaseContainerExtension() {
    override fun getComposeFile(): File {
        return resolveComposeFile("c4ang-infra/docker-compose/test/docker-compose-integration-test.yml")
    }

    override fun getSchemaFile(): File? {
        // JPA를 사용하므로 스키마 파일 불필요
        return null
    }
}

/**
 * 하위 호환성을 위한 별칭
 * 기존 테스트 코드에서 SharedContainerExtension을 사용하는 경우를 대비
 */
@Deprecated(
    message = "Use CustomerServiceContainerExtension instead",
    replaceWith = ReplaceWith("CustomerServiceContainerExtension")
)
typealias SharedContainerExtension = CustomerServiceContainerExtension
