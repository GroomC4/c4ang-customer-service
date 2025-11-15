package com.groom.customer.adapter.out.client

import com.groom.customer.domain.model.NewStore
import com.groom.customer.domain.port.CreateStorePort
import com.groom.customer.outbound.client.StoreClient
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Store 서비스 호출을 위한 Client Adapter.
 * CreateStorePort를 구현하여 외부 서비스와 Domain을 연결합니다.
 *
 * StoreClient 구현체는 프로파일에 따라 자동으로 결정됩니다:
 * - test: MockStoreClient (테스트용 Mock)
 * - prod: StoreFeignClient (실제 Feign 클라이언트)
 */
@Component
class StoreClientAdapter(
    private val storeClient: StoreClient,
) : CreateStorePort {
    override fun createNewStore(
        ownerUserId: UUID,
        name: String,
        description: String?,
    ): NewStore {
        val response =
            storeClient.create(
                ownerUserId = ownerUserId,
                name = name,
                description = description,
            )

        return NewStore(
            id = response.id,
            name = response.name,
        )
    }
}
