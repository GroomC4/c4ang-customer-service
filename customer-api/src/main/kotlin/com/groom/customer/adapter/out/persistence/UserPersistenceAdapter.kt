package com.groom.customer.adapter.out.persistence

import com.groom.customer.common.enums.UserRole
import com.groom.customer.domain.model.User
import com.groom.customer.domain.port.LoadUserPort
import com.groom.customer.domain.port.SaveUserPort
import com.groom.customer.outbound.repository.UserRepositoryImpl
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * User 영속성을 처리하는 Adapter.
 * LoadUserPort와 SaveUserPort를 구현하여 Domain이 필요로 하는 User 영속성 계약을 제공합니다.
 */
@Component
class UserPersistenceAdapter(
    private val userJpaRepository: UserRepositoryImpl,
) : LoadUserPort, SaveUserPort {

    override fun loadByEmail(email: String): User? {
        return userJpaRepository.findByEmail(email).orElse(null)
    }

    override fun loadByEmailAndRole(
        email: String,
        role: UserRole,
    ): User? {
        return userJpaRepository.findByEmailAndRole(email, role).orElse(null)
    }

    override fun loadById(userId: UUID): User? {
        return userJpaRepository.findById(userId).orElse(null)
    }

    override fun save(user: User): User {
        return userJpaRepository.save(user)
    }

    override fun existsByEmailAndRole(
        email: String,
        role: UserRole,
    ): Boolean {
        return userJpaRepository.existsByEmailIsAndRoleIs(email, role)
    }
}
