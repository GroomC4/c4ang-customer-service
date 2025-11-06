package com.groom.customer.application.service

import com.groom.customer.application.dto.RefreshTokenCommand
import com.groom.customer.application.dto.RefreshTokenResult
import com.groom.customer.application.dto.toRefreshTokenResult
import com.groom.customer.domain.service.Authenticator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Refresh Token 애플리케이션 서비스
 * 도메인 오케스트레이션에만 집중합니다.
 */
@Service
class RefreshTokenService(
    private val authenticator: Authenticator,
) {
    /**
     * 리프레시 토큰을 사용하여 새로운 인증 정보를 발급합니다.
     * 동시 로그인 후 토큰갱신 요청을 할 경우 복제지연으로 인해 이전 토큰이 유효하다고 판단될 수 있으므로
     * primary db의 트랜잭션을 걸어 일관성을 유지합니다.
     */
    @Transactional
    fun refresh(command: RefreshTokenCommand): RefreshTokenResult =
        authenticator
            .refreshCredentials(command.refreshToken)
            .toRefreshTokenResult()
}
