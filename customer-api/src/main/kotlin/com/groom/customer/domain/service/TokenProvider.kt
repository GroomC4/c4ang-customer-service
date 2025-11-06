package com.groom.customer.domain.service

import com.groom.customer.domain.model.User

/**
 * 토큰 생성을 담당하는 인프라 어댑터 인터페이스
 * 실제 토큰 생성 기술(JWT, OAuth 등)은 인프라 레이어에서 구현합니다.
 */
interface TokenProvider {
    /**
     * Access Token을 생성합니다.
     */
    fun generateAccessToken(user: User): String

    /**
     * Refresh Token을 생성합니다.
     */
    fun generateRefreshToken(user: User): String

    /**
     * Refresh Token을 검증합니다.
     */
    fun validateRefreshToken(token: String)

    /**
     * Access Token의 유효 시간(초)을 반환합니다.
     */
    fun getAccessTokenValiditySeconds(): Long

    /**
     * Refresh Token의 유효 시간(초)을 반환합니다.
     */
    fun getRefreshTokenValiditySeconds(): Long
}
