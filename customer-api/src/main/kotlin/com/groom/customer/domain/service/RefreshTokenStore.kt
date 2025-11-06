package com.groom.customer.domain.service

import com.groom.customer.domain.model.RefreshToken
import java.util.Optional
import java.util.UUID

/**
 * RefreshToken 저장소 인프라 어댑터 인터페이스
 * 실제 저장 기술(DB, Redis 등)은 인프라 레이어에서 구현합니다.
 */
interface RefreshTokenStore {
    /**
     * 사용자 ID로 RefreshToken을 조회합니다.
     */
    fun findByUserId(userId: UUID): Optional<RefreshToken>

    /**
     * RefreshToken을 저장합니다.
     */
    fun save(refreshToken: RefreshToken): RefreshToken

    /**
     * RefreshToken을 토큰 값으로 조회합니다.
     */
    fun findByToken(token: String): Optional<RefreshToken>
}
