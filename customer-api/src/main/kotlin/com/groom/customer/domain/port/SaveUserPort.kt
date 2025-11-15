package com.groom.customer.domain.port

import com.groom.customer.domain.model.User

/**
 * User 저장을 위한 Outbound Port.
 * Domain이 외부 영속성 계층에 요구하는 계약.
 */
interface SaveUserPort {
    /**
     * User 저장
     *
     * @param user 저장할 User
     * @return 저장된 User
     */
    fun save(user: User): User
}
