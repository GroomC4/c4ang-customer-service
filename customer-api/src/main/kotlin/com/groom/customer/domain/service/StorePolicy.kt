package com.groom.customer.domain.service

import java.util.UUID

interface StorePolicy {
    fun checkStoreAlreadyExists(id: UUID)
}
