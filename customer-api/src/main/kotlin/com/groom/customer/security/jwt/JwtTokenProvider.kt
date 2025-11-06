package com.groom.customer.security.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.AlgorithmMismatchException
import com.auth0.jwt.exceptions.InvalidClaimException
import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.exceptions.SignatureVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.groom.customer.common.exception.TokenException
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID
import com.auth0.jwt.exceptions.TokenExpiredException as Auth0TokenExpiredException

@Component
class JwtTokenProvider(
    private val properties: JwtProperties,
    private val clock: Clock,
) {
    private val algorithm: Algorithm = Algorithm.HMAC256(properties.secret)

    companion object {
        private const val EXPECTED_ALGORITHM = "HS256"
        private val BLOCKED_ALGORITHMS = setOf("none", "None", "NONE")
    }

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

    /**
     * JWT 토큰의 유효성을 검증하고 인증 데이터를 반환합니다.
     *
     * 검증 단계:
     * 1. 헤더 검증: 알고리즘 타입 확인 → TokenException.InvalidTokenAlgorithm 또는 TokenException.InvalidTokenFormat 가능
     * 2. 서명 검증: JWT 서명 및 만료 시간 확인 → TokenException.TokenExpired 또는 TokenException.InvalidTokenSignature 가능
     * 3. 클레임 검증: 필수 클레임 존재 여부 확인 → TokenException.MissingTokenClaim 가능
     *
     * @throws TokenException.TokenExpired 토큰이 만료된 경우 (클라이언트는 리프레시 플로우 시작)
     * @throws TokenException 기타 모든 검증 실패 (클라이언트는 재로그인)
     */
    fun validateToken(token: String): AuthorizationData {
        // 1. 헤더 검증 (알고리즘 혼동 공격 방어)
        validateTokenHeader(token)

        // 2. 서명 검증 및 클레임 추출 (만료, 서명 불일치 등)
        val decodedJWT = verifyTokenSignature(token)

        // 3. 필수 클레임 검증 (Subject, Role, JWT ID)
        validateRequiredClaims(decodedJWT)

        // 4. 인증 데이터 반환
        return extractAuthorizationData(decodedJWT)
    }

    /**
     * JWT 토큰의 서명을 검증합니다.
     * auth0 라이브러리의 다양한 예외를 커스텀 예외로 변환하여 일관된 예외 처리를 제공합니다.
     */
    private fun verifyTokenSignature(token: String): DecodedJWT =
        try {
            JWT
                .require(algorithm)
                .withIssuer(properties.issuer)
                .build()
                .verify(token)
        } catch (e: Auth0TokenExpiredException) {
            // 토큰 만료 - 클라이언트가 리프레시 플로우를 시작할 수 있도록 구분된 예외 사용
            throw TokenException.TokenExpired()
        } catch (e: SignatureVerificationException) {
            // 서명 검증 실패 - 보안을 위해 구체적인 사유는 로그에만 기록
            throw TokenException.InvalidTokenSignature(cause = e)
        } catch (e: AlgorithmMismatchException) {
            // 알고리즘 불일치 - 보안을 위해 구체적인 사유는 로그에만 기록
            throw TokenException.InvalidTokenAlgorithm(cause = e)
        } catch (e: InvalidClaimException) {
            // 클레임 유효성 검증 실패 (Issuer 불일치 등)
            // Issuer 관련 예외인 경우 InvalidTokenIssuer로 변환
            val message = e.message.orEmpty()
            if (message.contains("issuer", ignoreCase = true)) {
                // Issuer 정보 추출 시도
                throw TokenException.InvalidTokenIssuer(
                    expected = properties.issuer,
                    actual = "unknown", // Auth0 라이브러리는 실제값을 제공하지 않음
                )
            } else {
                throw TokenException.InvalidTokenFormat(cause = e)
            }
        } catch (e: JWTDecodeException) {
            // 디코딩 실패 - 형식이 잘못된 토큰
            throw TokenException.InvalidTokenFormat(cause = e)
        } catch (e: Exception) {
            // 예상하지 못한 예외 처리
            when (e) {
                is TokenException -> throw e
                else -> throw TokenException.InvalidTokenFormat(cause = e)
            }
        }

    /**
     * 검증된 JWT에서 인증 데이터를 추출합니다.
     */
    private fun extractAuthorizationData(decodedJWT: DecodedJWT) =
        AuthorizationData(
            id = decodedJWT.subject,
            roleName = decodedJWT.getClaim("role").asString(),
        )

    /**
     * JWT 헤더를 검증하여 알고리즘 혼동 공격을 방지합니다.
     *
     * 방어 메커니즘:
     * 1. 토큰 디코딩 가능 여부 확인
     * 2. None 알고리즘 차단
     * 3. 예상된 알고리즘(HS256)만 허용
     */
    private fun validateTokenHeader(token: String) {
        val decodedJWT =
            try {
                JWT.decode(token)
            } catch (e: JWTDecodeException) {
                throw TokenException.InvalidTokenFormat(cause = e)
            }

        val algorithm = decodedJWT.algorithm

        // None 알고리즘 차단
        if (algorithm in BLOCKED_ALGORITHMS) {
            throw TokenException.InvalidTokenAlgorithm(
                cause = IllegalArgumentException("None algorithm is not allowed"),
            )
        }

        // 예상된 알고리즘만 허용 (알고리즘 혼동 공격 방지)
        if (algorithm != EXPECTED_ALGORITHM) {
            throw TokenException.InvalidTokenAlgorithm(
                cause = IllegalArgumentException("Expected $EXPECTED_ALGORITHM but got $algorithm"),
            )
        }
    }

    /**
     * JWT의 필수 클레임을 검증합니다.
     */
    private fun validateRequiredClaims(decodedJWT: DecodedJWT) {
        // Subject (사용자 ID) 검증
        if (decodedJWT.subject.isNullOrBlank()) {
            throw TokenException.MissingTokenClaim(claimName = "subject")
        }

        // Role 클레임 검증
        val role = decodedJWT.getClaim("role").asString()
        if (role.isNullOrBlank()) {
            throw TokenException.MissingTokenClaim(claimName = "role")
        }

        // JWT ID 검증
        if (decodedJWT.id.isNullOrBlank()) {
            throw TokenException.MissingTokenClaim(claimName = "jti")
        }
    }
}
