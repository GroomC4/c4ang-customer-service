package com.groom.customer.application.service

import com.groom.customer.application.dto.RegisterManagerCommand
import com.groom.customer.application.dto.RegisterManagerResult
import com.groom.customer.domain.model.Email
import com.groom.customer.domain.model.PhoneNumber
import com.groom.customer.domain.model.UserRole
import com.groom.customer.domain.model.Username
import com.groom.customer.domain.port.SaveUserPort
import com.groom.customer.domain.service.UserFactory
import com.groom.customer.domain.service.UserPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegisterManagerService(
    private val saveUserPort: SaveUserPort,
    private val userPolicy: UserPolicy,
    private val userFactory: UserFactory,
) {
    @Transactional
    fun register(command: RegisterManagerCommand): RegisterManagerResult {
        userPolicy.checkAlreadyRegister(command.email, UserRole.MANAGER)

        val username = Username(command.username)
        val email = Email(command.email)
        val phoneNumber = PhoneNumber(command.phoneNumber)

        val newManager =
            userFactory
                .createNewManager(
                    username = username,
                    email = email,
                    passwordHash = command.rawPassword,
                    phoneNumber = phoneNumber,
                ).let(saveUserPort::save)

        return RegisterManagerResult(
            userId = newManager.id!!.toString(),
            username = newManager.username,
            email = newManager.email,
            role = newManager.role,
            isActive = newManager.isActive,
            createdAt = newManager.createdAt!!,
        )
    }
}
