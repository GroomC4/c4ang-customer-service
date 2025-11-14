package com.groom.customer.domain.port

import com.groom.customer.domain.model.RefreshToken
import java.util.UUID

/**
 * RefreshToken 조회를 위한 Outbound Port.
 * Domain이 외부 영속성 계층에 요구하는 계약.
 */
interface LoadRefreshTokenPort {
    /**
     * 사용자 ID로 RefreshToken을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return RefreshToken 엔티티 (없으면 null)
     */
    fun loadByUserId(userId: UUID): RefreshToken?

    /**
     * 토큰 값으로 RefreshToken을 조회합니다.
     *
     * @param token 토큰 값
     * @return RefreshToken 엔티티 (없으면 null)
     */
    fun loadByToken(token: String): RefreshToken?
}
