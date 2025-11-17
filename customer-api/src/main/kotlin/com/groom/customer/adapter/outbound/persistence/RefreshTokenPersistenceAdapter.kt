package com.groom.customer.adapter.outbound.persistence

import com.groom.customer.domain.model.RefreshToken
import com.groom.customer.domain.port.LoadRefreshTokenPort
import com.groom.customer.domain.port.SaveRefreshTokenPort
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * RefreshToken 영속성을 위한 Persistence Adapter.
 * LoadRefreshTokenPort와 SaveRefreshTokenPort를 구현하여 JPA Repository와 Domain을 연결합니다.
 */
@Component
class RefreshTokenPersistenceAdapter(
    private val refreshTokenRepository: RefreshTokenRepository,
) : LoadRefreshTokenPort,
    SaveRefreshTokenPort {
    override fun loadByUserId(userId: UUID): RefreshToken? =
        refreshTokenRepository
            .findByUserId(userId)
            .orElse(null)

    override fun loadByToken(token: String): RefreshToken? =
        refreshTokenRepository
            .findByToken(token)
            .orElse(null)

    override fun save(refreshToken: RefreshToken): RefreshToken = refreshTokenRepository.save(refreshToken)
}
