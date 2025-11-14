package com.groom.customer.domain.port

import java.util.UUID

/**
 * Store 생성을 위한 Outbound Port.
 * Domain이 외부 Store 서비스에 요구하는 계약.
 */
interface CreateStorePort {
    /**
     * 새로운 Store를 생성합니다.
     *
     * @param ownerUserId 판매자 사용자 ID
     * @param name 상점 이름
     * @param description 상점 설명
     * @return 생성된 Store 정보
     */
    fun createNewStore(
        ownerUserId: UUID,
        name: String,
        description: String?,
    ): NewStore
}

/**
 * 생성된 Store 정보
 */
data class NewStore(
    val id: UUID,
    val name: String,
)
