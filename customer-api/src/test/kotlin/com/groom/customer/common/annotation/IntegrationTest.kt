package com.groom.customer.common.annotation

import com.groom.infra.testcontainers.K8sContainerExtension
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * 통합 테스트를 위한 어노테이션
 *
 * Testcontainers K3s Module을 사용하여 K8s 환경을 제공합니다.
 * 기존 docker-compose 기반 통합 테스트를 K8s 기반으로 통합했습니다.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Tag("integration-test")
@ActiveProfiles("k8s-test")
@SpringBootTest
@ExtendWith(K8sContainerExtension::class)
annotation class IntegrationTest
