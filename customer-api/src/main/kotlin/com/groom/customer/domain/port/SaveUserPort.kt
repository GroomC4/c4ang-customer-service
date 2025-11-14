package com.groom.customer.domain.port

import com.groom.customer.common.enums.UserRole
import com.groom.customer.domain.model.User

/**
 * User 저장을 위한 Outbound Port.
 * Domain이 외부 영속성 계층에 요구하는 계약.
 */
interface SaveUserPort {
    /**
     * User를 저장합니다.
     *
     * @param user 저장할 User 엔티티
     * @return 저장된 User 엔티티
     */
    fun save(user: User): User

    /**
     * 이메일과 역할로 사용자가 이미 존재하는지 확인합니다.
     *
     * @param email 이메일
     * @param role 역할
     * @return 존재 여부
     */
    fun existsByEmailAndRole(
        email: String,
        role: UserRole,
    ): Boolean
}
