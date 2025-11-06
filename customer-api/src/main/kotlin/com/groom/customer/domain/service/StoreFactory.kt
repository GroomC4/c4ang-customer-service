package com.groom.customer.domain.service

import java.util.UUID

interface StoreFactory {
    fun createNewStore(
        ownerUserId: UUID,
        name: String,
        description: String?,
    ): NewStore
}

data class NewStore(
    val id: UUID,
    val name: String,
)
