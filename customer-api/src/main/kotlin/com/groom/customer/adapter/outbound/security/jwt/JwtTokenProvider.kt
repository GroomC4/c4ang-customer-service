package com.groom.customer.adapter.outbound.security.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.groom.customer.configuration.jwt.JwtProperties
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID

/**
 * JWT 토큰 발급 전용 Provider
 *
 * 이 서비스는 JWT 토큰 발급만 담당하며, 검증은 Istio API Gateway가 처리합니다.
 */
@Component
class JwtTokenProvider(
    private val properties: JwtProperties,
    private val clock: Clock,
) {
    private val algorithm: Algorithm = Algorithm.HMAC256(properties.secret)

    fun generateAccessToken(data: AuthorizationData): String {
        val issuedAt = Instant.now(clock)
        val expiresAt = issuedAt.plus(properties.accessTokenExpirationMinutes, ChronoUnit.MINUTES)

        return JWT
            .create()
            .withIssuer(properties.issuer)
            .withSubject(data.id)
            .withClaim("role", data.roleName)
            .withIssuedAt(Date.from(issuedAt))
            .withExpiresAt(Date.from(expiresAt))
            .withJWTId(
                UUID
                    .randomUUID()
                    .toString(),
            ) // 고유 ID 추가로 매번 다른 토큰 생성
            .sign(algorithm)
    }

    fun generateRefreshToken(data: AuthorizationData): String {
        val issuedAt = Instant.now(clock)
        val expiresAt = issuedAt.plus(properties.refreshTokenExpirationDays, ChronoUnit.DAYS)

        return JWT
            .create()
            .withIssuer(properties.issuer)
            .withSubject(data.id)
            .withClaim("role", data.roleName)
            .withIssuedAt(Date.from(issuedAt))
            .withExpiresAt(Date.from(expiresAt))
            .withJWTId(
                UUID
                    .randomUUID()
                    .toString(),
            ) // 고유 ID 추가로 매번 다른 토큰 생성
            .sign(algorithm)
    }
}
