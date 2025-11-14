package com.groom.customer.domain.port

import com.groom.customer.common.enums.UserRole
import com.groom.customer.domain.model.User
import java.util.UUID

/**
 * User 조회를 위한 Outbound Port.
 * Domain이 외부 영속성 계층에 요구하는 계약.
 */
interface LoadUserPort {
    /**
     * 이메일로 User를 조회합니다.
     *
     * @param email 사용자 이메일
     * @return User 엔티티 (없으면 null)
     */
    fun loadByEmail(email: String): User?

    /**
     * 이메일과 역할로 User를 조회합니다.
     *
     * @param email 사용자 이메일
     * @param role 사용자 역할
     * @return User 엔티티 (없으면 null)
     */
    fun loadByEmailAndRole(
        email: String,
        role: UserRole,
    ): User?

    /**
     * ID로 User를 조회합니다.
     *
     * @param userId 사용자 ID
     * @return User 엔티티 (없으면 null)
     */
    fun loadById(userId: UUID): User?
}
