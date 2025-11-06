package com.groom.customer.outbound.adapter

import com.groom.customer.domain.model.RefreshToken
import com.groom.customer.domain.service.RefreshTokenStore
import com.groom.customer.outbound.repository.RefreshTokenRepositoryImpl
import org.springframework.stereotype.Component
import java.util.Optional
import java.util.UUID

/**
 * JPA 기반 RefreshTokenStore 구현체
 */
@Component
class JpaRefreshTokenStore(
    private val refreshTokenRepository: RefreshTokenRepositoryImpl,
) : RefreshTokenStore {
    override fun findByUserId(userId: UUID): Optional<RefreshToken> = refreshTokenRepository.findByUserId(userId)

    override fun save(refreshToken: RefreshToken): RefreshToken = refreshTokenRepository.save(refreshToken)

    override fun findByToken(token: String): Optional<RefreshToken> = refreshTokenRepository.findByToken(token)
}
