package com.groom.customer.adapter.out.client

import com.groom.customer.domain.port.CreateStorePort
import com.groom.customer.domain.port.NewStore
import com.groom.customer.outbound.client.StoreClient
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * 외부 Store 서비스를 호출하는 Adapter.
 * CreateStorePort를 구현하여 Domain이 필요로 하는 Store 생성 계약을 제공합니다.
 *
 * @Profile("!test") - test 프로파일이 아닐 때만 활성화
 *                     (local, dev, prod 등에서 사용)
 */
@Component
@Profile("!test")
class StoreClientAdapter(
    private val storeClient: StoreClient,
) : CreateStorePort {

    override fun createNewStore(
        ownerUserId: UUID,
        name: String,
        description: String?,
    ): NewStore {
        val response = storeClient.create(
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
