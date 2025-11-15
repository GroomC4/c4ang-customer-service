package com.groom.customer.adapter.outbound.persistence

import com.groom.customer.adapter.outbound.persistence.UserRepositoryImpl
import com.groom.customer.common.enums.UserRole
import com.groom.customer.domain.model.User
import com.groom.customer.domain.port.LoadUserPort
import com.groom.customer.domain.port.SaveUserPort
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * User 조회 및 저장을 위한 Persistence Adapter.
 * LoadUserPort와 SaveUserPort를 구현하여 JPA Repository와 Domain을 연결합니다.
 */
@Component
class UserPersistenceAdapter(
    private val userRepository: UserRepositoryImpl,
) : LoadUserPort,
    SaveUserPort {
    override fun loadByEmail(email: String): User? =
        userRepository
            .findByEmail(email)
            .orElse(null)

    override fun loadByEmailAndRole(
        email: String,
        role: UserRole,
    ): User? =
        userRepository
            .findByEmailAndRole(email, role)
            .orElse(null)

    override fun loadById(userId: UUID): User? =
        userRepository
            .findById(userId)
            .orElse(null)

    override fun save(user: User): User = userRepository.save(user)
}
