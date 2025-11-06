package com.groom.customer.domain.service

import com.groom.customer.common.enums.UserRole
import com.groom.customer.domain.model.User
import java.util.Optional
import java.util.UUID

interface UserReader {
    fun findByEmail(email: String): Optional<User>

    fun findByEmailAndRole(
        email: String,
        role: UserRole,
    ): Optional<User>

    fun findById(userId: UUID): Optional<User>
}
