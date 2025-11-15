package com.groom.customer.domain.model

import java.util.UUID

/**
 * 새로 생성된 Store 정보
 * 외부 Store 서비스로부터 받은 응답 데이터
 */
data class NewStore(
    val id: UUID,
    val name: String,
)
