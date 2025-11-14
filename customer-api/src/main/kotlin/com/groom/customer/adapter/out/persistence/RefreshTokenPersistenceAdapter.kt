package com.groom.customer.adapter.out.persistence

import com.groom.customer.domain.model.RefreshToken
import com.groom.customer.domain.port.LoadRefreshTokenPort
import com.groom.customer.domain.port.SaveRefreshTokenPort
import com.groom.customer.outbound.repository.RefreshTokenRepositoryImpl
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * RefreshToken 영속성을 처리하는 Adapter.
 * LoadRefreshTokenPort와 SaveRefreshTokenPort를 구현하여
 * Domain이 필요로 하는 RefreshToken 영속성 계약을 제공합니다.
 */
@Component
class RefreshTokenPersistenceAdapter(
    private val refreshTokenJpaRepository: RefreshTokenRepositoryImpl,
) : LoadRefreshTokenPort, SaveRefreshTokenPort {

    override fun loadByUserId(userId: UUID): RefreshToken? {
        return refreshTokenJpaRepository.findByUserId(userId).orElse(null)
    }

    override fun loadByToken(token: String): RefreshToken? {
        return refreshTokenJpaRepository.findByToken(token).orElse(null)
    }

    override fun save(refreshToken: RefreshToken): RefreshToken {
        return refreshTokenJpaRepository.save(refreshToken)
    }
}
