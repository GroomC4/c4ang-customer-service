package com.groom.customer.domain.port

import com.groom.customer.domain.model.User

/**
 * 토큰 생성을 위한 Outbound Port.
 * Domain이 외부 인프라(JWT, OAuth 등)에 요구하는 계약.
 */
interface GenerateTokenPort {
    /**
     * Access Token을 생성합니다.
     *
     * @param user 토큰을 생성할 사용자
     * @return 생성된 Access Token
     */
    fun generateAccessToken(user: User): String

    /**
     * Refresh Token을 생성합니다.
     *
     * @param user 토큰을 생성할 사용자
     * @return 생성된 Refresh Token
     */
    fun generateRefreshToken(user: User): String

    /**
     * Refresh Token을 검증합니다.
     *
     * @param token 검증할 Refresh Token
     * @throws IllegalArgumentException 토큰이 유효하지 않을 경우
     */
    fun validateRefreshToken(token: String)

    /**
     * Access Token의 유효 시간(초)을 반환합니다.
     *
     * @return Access Token 유효 시간(초)
     */
    fun getAccessTokenValiditySeconds(): Long

    /**
     * Refresh Token의 유효 시간(초)을 반환합니다.
     *
     * @return Refresh Token 유효 시간(초)
     */
    fun getRefreshTokenValiditySeconds(): Long
}
