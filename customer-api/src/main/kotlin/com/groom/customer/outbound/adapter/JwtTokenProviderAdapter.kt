package com.groom.customer.outbound.adapter

import com.groom.customer.domain.model.User
import com.groom.customer.domain.service.TokenProvider
import com.groom.customer.security.jwt.AuthorizationData
import com.groom.customer.security.jwt.JwtProperties
import com.groom.customer.security.jwt.JwtTokenProvider
import org.springframework.stereotype.Component

/**
 * JWT 기반 TokenProvider 구현체
 */
@Component
class JwtTokenProviderAdapter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val properties: JwtProperties,
) : TokenProvider {
    override fun generateAccessToken(user: User): String =
        jwtTokenProvider.generateAccessToken(
            AuthorizationData(
                id = user.id.toString(),
                roleName = user.role.name,
            ),
        )

    override fun generateRefreshToken(user: User): String =
        jwtTokenProvider.generateRefreshToken(
            AuthorizationData(
                id = user.id.toString(),
                roleName = user.role.name,
            ),
        )

    override fun validateRefreshToken(token: String) {
        jwtTokenProvider.validateToken(token)
    }

    override fun getAccessTokenValiditySeconds(): Long = properties.accessTokenExpirationMinutes * 60

    override fun getRefreshTokenValiditySeconds(): Long = properties.refreshTokenExpirationDays * 24 * 60 * 60
}
