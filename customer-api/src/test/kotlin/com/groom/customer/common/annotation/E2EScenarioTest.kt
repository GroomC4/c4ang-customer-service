package com.groom.customer.common.annotation

import com.groom.customer.common.extension.SharedContainerExtension
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.ActiveProfiles

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Tag("e2e-test")
@ActiveProfiles("test")
@ExtendWith(SharedContainerExtension::class)
annotation class E2EScenarioTest
