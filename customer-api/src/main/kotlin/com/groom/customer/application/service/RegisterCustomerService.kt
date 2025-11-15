package com.groom.customer.application.service

import com.groom.customer.application.dto.RegisterCustomerCommand
import com.groom.customer.application.dto.RegisterCustomerResult
import com.groom.customer.common.enums.UserRole
import com.groom.customer.domain.model.Address
import com.groom.customer.domain.model.Email
import com.groom.customer.domain.model.PhoneNumber
import com.groom.customer.domain.model.Username
import com.groom.customer.domain.port.SaveUserPort
import com.groom.customer.domain.service.UserFactory
import com.groom.customer.domain.service.UserPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegisterCustomerService(
    private val saveUserPort: SaveUserPort,
    private val userPolicy: UserPolicy,
    private val userFactory: UserFactory,
) {
    @Transactional
    fun register(command: RegisterCustomerCommand): RegisterCustomerResult {
        userPolicy.checkAlreadyRegister(command.email, UserRole.CUSTOMER)

        val username = Username(command.username)
        val email = Email(command.email)
        val address = Address(command.defaultAddress)
        val phoneNumber = PhoneNumber(command.defaultPhoneNumber)

        val newCustomer =
            userFactory
                .createNewCustomer(
                    username = username,
                    email = email,
                    passwordHash = command.rawPassword,
                    defaultAddress = address,
                    defaultPhoneNumber = phoneNumber,
                ).let(saveUserPort::save)

        return RegisterCustomerResult(
            userId = newCustomer.id!!.toString(),
            username = newCustomer.username,
            email = newCustomer.email,
            role = newCustomer.role,
            isActive = newCustomer.isActive,
            createdAt = newCustomer.createdAt!!,
        )
    }
}
