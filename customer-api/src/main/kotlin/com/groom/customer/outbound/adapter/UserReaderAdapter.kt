package com.groom.customer.outbound.adapter

import com.groom.customer.common.enums.UserRole
import com.groom.customer.domain.model.User
import com.groom.customer.domain.service.UserReader
import com.groom.customer.outbound.repository.UserRepositoryImpl
import org.springframework.stereotype.Component
import java.util.Optional
import java.util.UUID

@Component
class UserReaderAdapter(
    private val userRepositoryImpl: UserRepositoryImpl,
) : UserReader {
    override fun findByEmail(email: String): Optional<User> = userRepositoryImpl.findByEmail(email)

    override fun findByEmailAndRole(
        email: String,
        role: UserRole,
    ): Optional<User> = userRepositoryImpl.findByEmailAndRole(email, role)

    override fun findById(userId: UUID): Optional<User> = userRepositoryImpl.findById(userId)
}
