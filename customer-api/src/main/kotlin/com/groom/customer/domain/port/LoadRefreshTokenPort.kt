package com.groom.customer.domain.port

import com.groom.customer.domain.model.RefreshToken
import java.util.UUID

/**
 * RefreshToken 조회를 위한 Outbound Port.
 * Domain이 외부 인프라에 요구하는 계약.
 */
interface LoadRefreshTokenPort {
    /**
     * 사용자 ID로 RefreshToken을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return RefreshToken 또는 null (존재하지 않을 경우)
     */
    fun loadByUserId(userId: UUID): RefreshToken?

    /**
     * RefreshToken을 토큰 값으로 조회합니다.
     *
     * @param token 토큰 값
     * @return RefreshToken 또는 null (존재하지 않을 경우)
     */
    fun loadByToken(token: String): RefreshToken?
}
