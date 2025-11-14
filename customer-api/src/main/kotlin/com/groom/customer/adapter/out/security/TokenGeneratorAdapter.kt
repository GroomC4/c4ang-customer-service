package com.groom.customer.adapter.out.security

import com.groom.customer.domain.model.User
import com.groom.customer.domain.port.GenerateTokenPort
import com.groom.customer.security.jwt.AuthorizationData
import com.groom.customer.security.jwt.JwtProperties
import com.groom.customer.security.jwt.JwtTokenProvider
import org.springframework.stereotype.Component

/**
 * JWT 기반 토큰 생성 Adapter.
 * GenerateTokenPort를 구현하여 Domain이 필요로 하는 토큰 생성 계약을 제공합니다.
 */
@Component
class TokenGeneratorAdapter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val properties: JwtProperties,
) : GenerateTokenPort {

    override fun generateAccessToken(user: User): String {
        return jwtTokenProvider.generateAccessToken(
            AuthorizationData(
                id = user.id.toString(),
                roleName = user.role.name,
            ),
        )
    }

    override fun generateRefreshToken(user: User): String {
        return jwtTokenProvider.generateRefreshToken(
            AuthorizationData(
                id = user.id.toString(),
                roleName = user.role.name,
            ),
        )
    }

    override fun validateRefreshToken(token: String) {
        // Refresh Token 검증은 DB 조회로만 처리
        // - RefreshToken 엔티티 존재 여부 확인
        // - isRevoked 플래그 확인
        // JWT 자체 검증은 불필요 (DB가 신뢰할 수 있는 소스)
    }

    override fun getAccessTokenValiditySeconds(): Long {
        return properties.accessTokenExpirationMinutes * 60
    }

    override fun getRefreshTokenValiditySeconds(): Long {
        return properties.refreshTokenExpirationDays * 24 * 60 * 60
    }
}
