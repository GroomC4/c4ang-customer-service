package com.groom.customer.application.service

import com.groom.customer.application.dto.RegisterOwnerCommand
import com.groom.customer.application.dto.RegisterOwnerResult
import com.groom.customer.common.enums.UserRole
import com.groom.customer.domain.model.Email
import com.groom.customer.domain.model.PhoneNumber
import com.groom.customer.domain.model.Username
import com.groom.customer.domain.service.StoreFactory
import com.groom.customer.domain.service.StorePolicy
import com.groom.customer.domain.service.UserFactory
import com.groom.customer.domain.service.UserPolicy
import com.groom.customer.outbound.repository.UserRepositoryImpl
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegisterOwnerService(
    private val userRepository: UserRepositoryImpl,
    private val userPolicy: UserPolicy,
    private val userFactory: UserFactory,
    private val storePolicy: StorePolicy,
    private val storeFactory: StoreFactory,
) {
    @Transactional
    fun register(command: RegisterOwnerCommand): RegisterOwnerResult {
        // 이미 가입한 이메일인지 확인
        userPolicy.checkAlreadyRegister(command.email, UserRole.OWNER)

        val username = Username(command.username)
        val email = Email(command.email)
        val phoneNumber = PhoneNumber(command.phoneNumber)

        // 판매자 계정 생성
        val newOwner =
            userFactory
                .createNewOwner(
                    username = username,
                    email = email,
                    passwordHash = command.rawPassword,
                    phoneNumber = phoneNumber,
                ).let(userRepository::save)

        // 스토어 생성 전 중복 확인 (이미 스토어를 보유하고 있는지)
        storePolicy.checkStoreAlreadyExists(newOwner.id)

        // 스토어 생성
        val newStore =
            storeFactory
                .createNewStore(
                    ownerUserId = newOwner.id,
                    name = command.storeName,
                    description = command.storeDescription,
                )

        return RegisterOwnerResult(
            userId = newOwner.id.toString(),
            username = newOwner.username,
            email = newOwner.email,
            storeId = newStore.id.toString(),
            storeName = newStore.name,
            createdAt = newOwner.createdAt!!,
        )
    }
}
