package com.groom.customer.outbound.repository

import com.groom.customer.common.enums.UserRole
import com.groom.customer.domain.model.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface UserRepositoryImpl : JpaRepository<User, UUID> {
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
