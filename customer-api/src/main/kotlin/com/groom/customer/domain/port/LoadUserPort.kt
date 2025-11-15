package com.groom.customer.domain.port

import com.groom.customer.common.enums.UserRole
import com.groom.customer.domain.model.User
import java.util.UUID

/**
 * User 조회를 위한 Outbound Port.
 * Domain이 외부 인프라에 요구하는 계약.
 */
interface LoadUserPort {
    /**
     * 이메일로 User 조회
     *
     * @param email 사용자 이메일
     * @return User 또는 null (존재하지 않을 경우)
     */
    fun loadByEmail(email: String): User?

    /**
     * 이메일과 역할로 User 조회
     *
     * @param email 사용자 이메일
     * @param role 사용자 역할
     * @return User 또는 null (존재하지 않을 경우)
     */
    fun loadByEmailAndRole(
        email: String,
        role: UserRole,
    ): User?

    /**
     * ID로 User 조회
     *
     * @param userId 사용자 ID
     * @return User 또는 null (존재하지 않을 경우)
     */
    fun loadById(userId: UUID): User?
}
