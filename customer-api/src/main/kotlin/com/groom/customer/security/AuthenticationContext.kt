package com.groom.customer.security

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * 현재 인증된 사용자 정보를 제공하는 컨텍스트
 */
@Component
class AuthenticationContext {
    /**
     * 현재 인증된 사용자의 ID를 반환합니다.
     *
     * @return 사용자 ID (UUID)
     * @throws IllegalStateException 인증되지 않은 경우
     */
    fun getCurrentUserId(): UUID {
        val authentication =
            SecurityContextHolder.getContext().authentication
                ?: throw IllegalStateException("인증되지 않은 사용자입니다.")

        val userId =
            authentication.principal as? String
                ?: throw IllegalStateException("사용자 ID를 찾을 수 없습니다.")

        return try {
            UUID.fromString(userId)
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException("잘못된 사용자 ID 형식입니다: $userId", e)
        }
    }

    /**
     * 현재 인증된 사용자의 역할을 반환합니다.
     *
     * @return 사용자 역할 (예: "CUSTOMER", "OWNER")
     */
    fun getCurrentUserRole(): String {
        val authentication =
            SecurityContextHolder.getContext().authentication
                ?: throw IllegalStateException("인증되지 않은 사용자입니다.")

        // authorities에서 ROLE_ prefix 제거하여 반환
        return authentication.authorities
            .firstOrNull()
            ?.authority
            ?.removePrefix("ROLE_")
            ?: throw IllegalStateException("사용자 역할을 찾을 수 없습니다.")
    }

    /**
     * 현재 사용자가 인증되었는지 확인합니다.
     */
    fun isAuthenticated(): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication != null && authentication.isAuthenticated
    }
}
