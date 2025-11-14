package com.groom.customer.adapter.out.security

import com.groom.customer.domain.model.User
import com.groom.customer.domain.port.VerifyPasswordPort
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

/**
 * Bcrypt 기반 비밀번호 검증 Adapter.
 * VerifyPasswordPort를 구현하여 Domain이 필요로 하는 비밀번호 검증 계약을 제공합니다.
 */
@Component
class PasswordVerifierAdapter(
    private val passwordEncoder: PasswordEncoder,
) : VerifyPasswordPort {

    override fun verifyPassword(
        user: User,
        rawPassword: String,
    ): Boolean {
        return passwordEncoder.matches(rawPassword, user.passwordHash)
    }
}
