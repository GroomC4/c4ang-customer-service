package com.groom.customer.fixture

import com.groom.customer.domain.service.NewStore
import com.groom.customer.domain.service.StoreFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * 테스트 환경 전용 StoreFactory 구현체
 */
@Component
@Profile("test")
@Primary
class NoOpsStoreAdapter : StoreFactory {
    override fun createNewStore(
        ownerUserId: UUID,
        name: String,
        description: String?,
    ): NewStore {
        logger.info { "NoOpsStoreAdapter.createNewStore called (Test Mode)" }
        logger.debug { "ownerUserId=$ownerUserId, name=$name, description=$description" }

        return NewStore(
            id = UUID.randomUUID(),
            name = name,
        )
    }
}
