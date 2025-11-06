package com.groom.customer.outbound.repository

import com.groom.customer.domain.model.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface RefreshTokenRepositoryImpl : JpaRepository<RefreshToken, UUID> {
    fun findByUserId(userId: UUID): Optional<RefreshToken>

    fun findByToken(token: String): Optional<RefreshToken>
}
