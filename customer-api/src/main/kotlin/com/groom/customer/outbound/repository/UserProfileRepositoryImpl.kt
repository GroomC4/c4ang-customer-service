package com.groom.customer.outbound.repository

import com.groom.customer.domain.model.UserProfile
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface UserProfileRepositoryImpl : JpaRepository<UserProfile, UUID> {
    fun findByUser_Id(userId: UUID): Optional<UserProfile>
}
