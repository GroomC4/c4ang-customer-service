package com.groom.customer.adapter.outbound.security.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.groom.customer.configuration.jwt.JwtProperties
import org.springframework.stereotype.Component
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.Date
import java.util.UUID

/**
 * JWT 토큰 발급 전용 Provider (RSA256 알고리즘 사용)
 *
 * 이 서비스는 JWT 토큰 발급만 담당하며, 검증은 Istio API Gateway가 처리합니다.
 * RSA256 비대칭키를 사용하여 JWKS 엔드포인트를 통해 공개키를 노출할 수 있습니다.
 */
@Component
class JwtTokenProvider(
    private val properties: JwtProperties,
    private val clock: Clock,
) {
    private val privateKey: RSAPrivateKey = loadPrivateKey(properties.privateKey)
    val publicKey: RSAPublicKey = loadPublicKey(properties.publicKey)
    private val algorithm: Algorithm = Algorithm.RSA256(publicKey, privateKey)

    val keyId: String get() = properties.keyId

    fun generateAccessToken(data: AuthorizationData): String {
        val issuedAt = Instant.now(clock)
        val expiresAt = issuedAt.plus(properties.accessTokenExpirationMinutes, ChronoUnit.MINUTES)

        return JWT
            .create()
            .withKeyId(properties.keyId)
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
            .withKeyId(properties.keyId)
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

    private fun loadPrivateKey(pem: String): RSAPrivateKey {
        val keyContent = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replace("\\n", "") // 리터럴 \n 문자열 처리
            .replace("\\s".toRegex(), "") // 실제 공백/개행 처리

        val keyBytes = Base64.getDecoder().decode(keyContent)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePrivate(keySpec) as RSAPrivateKey
    }

    private fun loadPublicKey(pem: String): RSAPublicKey {
        val keyContent = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("-----BEGIN RSA PUBLIC KEY-----", "")
            .replace("-----END RSA PUBLIC KEY-----", "")
            .replace("\\n", "") // 리터럴 \n 문자열 처리
            .replace("\\s".toRegex(), "") // 실제 공백/개행 처리

        val keyBytes = Base64.getDecoder().decode(keyContent)
        val keySpec = X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(keySpec) as RSAPublicKey
    }
}
