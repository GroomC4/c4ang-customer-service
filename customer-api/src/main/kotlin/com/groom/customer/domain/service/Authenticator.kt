package com.groom.customer.domain.service

import com.groom.customer.common.exception.RefreshTokenException
import com.groom.customer.common.exception.UserException
import com.groom.customer.domain.model.RefreshToken
import com.groom.customer.domain.model.TokenCredentials
import com.groom.customer.domain.model.User
import com.groom.customer.domain.port.GenerateTokenPort
import com.groom.customer.domain.port.LoadRefreshTokenPort
import com.groom.customer.domain.port.LoadUserPort
import com.groom.customer.domain.port.SaveRefreshTokenPort
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 인증 도메인 서비스
 * 사용자 인증(로그인, 로그아웃, 토큰 갱신) 관련 비즈니스 로직을 처리합니다.
 */
@Service
class Authenticator(
    private val generateTokenPort: GenerateTokenPort,
    private val loadRefreshTokenPort: LoadRefreshTokenPort,
    private val saveRefreshTokenPort: SaveRefreshTokenPort,
    private val loadUserPort: LoadUserPort,
) {
    /**
     * 사용자의 인증 정보를 생성하고 저장 또는 갱신합니다.
     *
     * 비즈니스 로직:
     * - 기존 RefreshToken이 존재하면 갱신
     * - 존재하지 않으면 새로 생성
     */
    fun createAndPersistCredentials(
        user: User,
        clientIp: String?,
        now: LocalDateTime = LocalDateTime.now(),
    ): TokenCredentials {
        // 1. 토큰 생성
        val accessToken = generateTokenPort.generateAccessToken(user)
        val refreshToken = generateTokenPort.generateRefreshToken(user)

        // 2. RefreshToken 저장 또는 갱신 (도메인 로직)
        val userId = user.id
        val existingRefreshToken = loadRefreshTokenPort.loadByUserId(userId)

        if (existingRefreshToken != null) {
            // 기존 토큰이 있으면 갱신
            existingRefreshToken.updateToken(
                newToken = refreshToken,
                newExpiresAt = now.plusDays(generateTokenPort.getRefreshTokenValiditySeconds()),
            )
            saveRefreshTokenPort.save(existingRefreshToken)
        } else {
            // 새로운 토큰 생성
            val newRefreshToken =
                RefreshToken(
                    userId = userId,
                    token = refreshToken,
                    clientIp = clientIp,
                    expiresAt = now.plusDays(generateTokenPort.getRefreshTokenValiditySeconds()),
                )
            saveRefreshTokenPort.save(newRefreshToken)
        }

        // 3. 토큰 기반 인증 정보 생성
        return TokenCredentials(
            primaryToken = accessToken,
            secondaryToken = refreshToken,
            validitySeconds = generateTokenPort.getAccessTokenValiditySeconds(),
        )
    }

    /**
     * 사용자의 인증 정보를 무효화합니다 (로그아웃).
     *
     * 비즈니스 로직:
     * - RefreshToken이 이미 무효화되어 있으면 예외 발생
     */
    fun revokeCredentials(user: User) {
        val userId = user.id

        val refreshToken =
            loadRefreshTokenPort.loadByUserId(userId)
                ?: throw IllegalArgumentException("로그아웃할 수 없습니다. 유효한 세션이 존재하지 않습니다.")

        // 도메인 로직: 이미 무효화된 토큰 확인
        if (refreshToken.token == null) {
            throw IllegalArgumentException("이미 로그아웃된 상태입니다.")
        }

        refreshToken.invalidate()
        saveRefreshTokenPort.save(refreshToken)
    }

    /**
     * Refresh Token으로 새로운 Access Token을 발급합니다.
     *
     * 비즈니스 로직:
     * - DB에서 Refresh Token 조회 및 검증
     * - 토큰 유효성 검증 (만료, 무효화 여부)
     * - JWT 토큰 검증
     * - 새로운 Access Token 생성
     */
    fun refreshCredentials(refreshToken: String): TokenCredentials {
        // 1. DB에서 Refresh Token 조회
        val storedRefreshToken =
            loadRefreshTokenPort.loadByToken(refreshToken)
                ?: throw RefreshTokenException.RefreshTokenNotFound(tokenValue = refreshToken)

        // 2. Refresh Token 도메인 검증
        if (storedRefreshToken.token == null) {
            throw IllegalArgumentException("이미 무효화된 Refresh Token입니다.")
        }

        if (storedRefreshToken.isExpired(LocalDateTime.now())) {
            throw RefreshTokenException.RefreshTokenExpired()
        }

        // 3. JWT 토큰 검증
        generateTokenPort.validateRefreshToken(refreshToken)

        // 4. 사용자 정보 조회
        val user =
            loadUserPort.loadById(storedRefreshToken.userId)
                ?: throw UserException.UserNotFound(userId = storedRefreshToken.userId)

        // 5. 새로운 Access Token 생성
        val accessToken = generateTokenPort.generateAccessToken(user)

        return TokenCredentials(
            primaryToken = accessToken,
            secondaryToken = null, // Refresh 시에는 새 Refresh Token을 발급하지 않음
            validitySeconds = generateTokenPort.getAccessTokenValiditySeconds(),
        )
    }
}
