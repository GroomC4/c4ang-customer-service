package com.groom.customer.domain.port

import com.groom.customer.domain.model.User

/**
 * 토큰 생성을 위한 Outbound Port.
 * Domain이 외부 토큰 생성 시스템에 요구하는 계약.
 * 실제 토큰 생성 기술(JWT, OAuth 등)은 Adapter에서 구현합니다.
 */
interface GenerateTokenPort {
    /**
     * Access Token을 생성합니다.
     *
     * @param user 토큰을 생성할 User
     * @return Access Token 문자열
     */
    fun generateAccessToken(user: User): String

    /**
     * Refresh Token을 생성합니다.
     *
     * @param user 토큰을 생성할 User
     * @return Refresh Token 문자열
     */
    fun generateRefreshToken(user: User): String

    /**
     * Refresh Token을 검증합니다.
     *
     * @param token 검증할 Refresh Token
     */
    fun validateRefreshToken(token: String)

    /**
     * Access Token의 유효 시간(초)을 반환합니다.
     *
     * @return 유효 시간 (초)
     */
    fun getAccessTokenValiditySeconds(): Long

    /**
     * Refresh Token의 유효 시간(초)을 반환합니다.
     *
     * @return 유효 시간 (초)
     */
    fun getRefreshTokenValiditySeconds(): Long
}
