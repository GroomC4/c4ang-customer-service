package com.groom.customer.domain.model

/**
 * 인증 성공 결과를 나타내는 도메인 모델
 * 인증 방식에 따라 다른 구조를 가질 수 있습니다.
 */
sealed interface AuthenticationCredentials {
    /**
     * 인증 정보의 유효 시간을 초 단위로 반환합니다.
     */
    fun getValiditySeconds(): Long
}

/**
 * 토큰 기반 인증 결과 (JWT, OAuth2 등)
 */
data class TokenCredentials(
    val primaryToken: String, // 실제 인증에 사용되는 토큰
    val secondaryToken: String?, // 토큰 갱신용 (없을 수도 있음)
    private val validitySeconds: Long,
) : AuthenticationCredentials {
    override fun getValiditySeconds(): Long = validitySeconds
}
