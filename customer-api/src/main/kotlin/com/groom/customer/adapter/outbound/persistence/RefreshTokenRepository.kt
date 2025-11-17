package com.groom.customer.adapter.outbound.persistence

import com.groom.customer.domain.model.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {
    fun findByUserId(userId: UUID): Optional<RefreshToken>

    fun findByToken(token: String): Optional<RefreshToken>
}
