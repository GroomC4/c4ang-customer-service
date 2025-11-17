package com.groom.customer.adapter.outbound.persistence

import com.groom.customer.domain.model.User
import com.groom.customer.domain.model.UserRole
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmail(email: String): Optional<User>

    fun findByEmailAndRole(
        email: String,
        role: UserRole,
    ): Optional<User>

    fun existsByUsername(username: String): Boolean

    fun existsByEmail(email: String): Boolean

    fun existsByEmailIsAndRoleIs(
        email: String,
        role: UserRole,
    ): Boolean
}
