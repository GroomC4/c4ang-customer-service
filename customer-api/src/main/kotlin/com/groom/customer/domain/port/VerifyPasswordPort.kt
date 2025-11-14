package com.groom.customer.domain.port

import com.groom.customer.domain.model.User

/**
 * 비밀번호 검증을 위한 Outbound Port.
 * Domain이 외부 암호화 시스템에 요구하는 계약.
 * 실제 암호화 알고리즘(Bcrypt, Argon2 등)은 Adapter에서 구현합니다.
 */
interface VerifyPasswordPort {
    /**
     * 사용자의 비밀번호가 일치하는지 검증합니다.
     *
     * @param user 검증할 사용자
     * @param rawPassword 평문 비밀번호
     * @return 비밀번호 일치 여부
     */
    fun verifyPassword(
        user: User,
        rawPassword: String,
    ): Boolean
}
