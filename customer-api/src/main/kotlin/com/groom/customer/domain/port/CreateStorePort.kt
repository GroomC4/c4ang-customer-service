package com.groom.customer.domain.port

import com.groom.customer.domain.model.NewStore
import java.util.UUID

/**
 * Store 생성을 위한 Outbound Port.
 * Domain이 외부 서비스(Store Service)에 요구하는 계약.
 */
interface CreateStorePort {
    /**
     * 새로운 Store를 생성합니다.
     *
     * @param ownerUserId Store 소유자 ID
     * @param name Store 이름
     * @param description Store 설명
     * @return 생성된 Store 정보
     */
    fun createNewStore(
        ownerUserId: UUID,
        name: String,
        description: String?,
    ): NewStore
}
