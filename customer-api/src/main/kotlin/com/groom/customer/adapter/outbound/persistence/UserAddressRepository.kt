package com.groom.customer.adapter.outbound.persistence

import com.groom.customer.domain.model.UserAddress
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface UserAddressRepository : JpaRepository<UserAddress, UUID> {
    fun findByUser_Id(userId: UUID): List<UserAddress>

    fun findByUser_IdAndLabel(
        userId: UUID,
        label: String,
    ): Optional<UserAddress>

    fun findByUser_IdAndIsDefault(
        userId: UUID,
        isDefault: Boolean,
    ): Optional<UserAddress>

    fun existsByUser_IdAndLabel(
        userId: UUID,
        label: String,
    ): Boolean
}
