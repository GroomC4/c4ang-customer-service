package com.groom.customer.outbound.adapter

import com.groom.customer.domain.service.NewStore
import com.groom.customer.domain.service.StoreFactory
import com.groom.customer.outbound.client.StoreClient
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * 프로덕션 환경용 StoreFactory 구현체
 *
 * 실제 Store 서비스(REST API 또는 gRPC)를 호출하여 상점을 생성합니다.
 *
 * @Profile("!test") - test 프로파일이 아닐 때만 활성화
 *                     (local, dev, prod 등에서 사용)
 */
@Component
@Profile("!test")
class StoreFactoryAdapter(
    private val storeClient: StoreClient,
) : StoreFactory {
    override fun createNewStore(
        ownerUserId: UUID,
        name: String,
        description: String?
    ): NewStore {
        val response = storeClient.create(
            ownerUserId = ownerUserId,
            name = name,
            description = description
        )

        return NewStore(
            id = response.id,
            name = response.name,
        )
    }
}