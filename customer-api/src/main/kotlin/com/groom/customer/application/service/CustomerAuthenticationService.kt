package com.groom.customer.application.service

import com.groom.customer.application.dto.LoginCommand
import com.groom.customer.application.dto.LoginResult
import com.groom.customer.application.dto.LogoutCommand
import com.groom.customer.application.dto.toLoginResult
import com.groom.customer.domain.service.Authenticator
import com.groom.customer.domain.service.UserPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 일반 고객 인증 애플리케이션 서비스
 * 일반 고객의 로그인, 로그아웃 유스케이스를 담당합니다.
 * 순수 도메인 오케스트레이션만 수행합니다.
 */
@Service
class CustomerAuthenticationService(
    private val authenticator: Authenticator,
    private val userPolicy: UserPolicy,
) {
    /**
     * 일반 고객 로그인
     */
    @Transactional
    fun login(command: LoginCommand): LoginResult {
        val user = userPolicy.loadActiveCustomerByEmail(command.email)
        userPolicy.validatePassword(user, command.password, command.email)

        return authenticator
            .createAndPersistCredentials(user, command.clientIp)
            .toLoginResult()
    }

    /**
     * 일반 고객 로그아웃
     */
    @Transactional
    fun logout(command: LogoutCommand) {
        val user = userPolicy.loadCustomerById(command.userId)
        authenticator.revokeCredentials(user)
    }
}
