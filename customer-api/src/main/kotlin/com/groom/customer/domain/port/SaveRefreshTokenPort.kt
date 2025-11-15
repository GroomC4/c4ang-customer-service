package com.groom.customer.domain.port

import com.groom.customer.domain.model.RefreshToken

/**
 * RefreshToken 저장을 위한 Outbound Port.
 * Domain이 외부 인프라에 요구하는 계약.
 */
interface SaveRefreshTokenPort {
    /**
     * RefreshToken을 저장합니다.
     *
     * @param refreshToken 저장할 RefreshToken
     * @return 저장된 RefreshToken
     */
    fun save(refreshToken: RefreshToken): RefreshToken
}
