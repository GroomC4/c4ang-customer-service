package com.groom.customer.domain.port

import com.groom.customer.domain.model.RefreshToken

/**
 * RefreshToken 저장을 위한 Outbound Port.
 * Domain이 외부 영속성 계층에 요구하는 계약.
 */
interface SaveRefreshTokenPort {
    /**
     * RefreshToken을 저장합니다.
     *
     * @param refreshToken 저장할 RefreshToken 엔티티
     * @return 저장된 RefreshToken 엔티티
     */
    fun save(refreshToken: RefreshToken): RefreshToken
}
