package com.groom.customer.outbound.adapter

import com.groom.customer.domain.model.User
import com.groom.customer.domain.service.PasswordVerifier
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

/**
 * Bcrypt 기반 비밀번호 검증 구현체
 */
@Service
class BcryptPasswordVerifier(
    private val passwordEncoder: PasswordEncoder,
) : PasswordVerifier {
    override fun verifyPassword(
        user: User,
        rawPassword: String,
    ): Boolean = passwordEncoder.matches(rawPassword, user.passwordHash)
}
