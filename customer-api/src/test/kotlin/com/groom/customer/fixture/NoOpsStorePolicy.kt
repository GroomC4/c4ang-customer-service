package com.groom.customer.fixture

import com.groom.customer.domain.service.StorePolicy
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * 테스트 환경 전용 StorePolicy 구현체
 */
@Component
@Profile("test")
@Primary
class NoOpsStorePolicy : StorePolicy {
    override fun checkStoreAlreadyExists(id: UUID) {
        return
    }
}