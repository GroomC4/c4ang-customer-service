package com.groom.customer.application.service

import com.groom.customer.application.dto.LoginCommand
import com.groom.customer.application.dto.LoginResult
import com.groom.customer.application.dto.LogoutCommand
import com.groom.customer.application.dto.toLoginResult
import com.groom.customer.common.exception.AuthenticationException
import com.groom.customer.common.exception.UserException
import com.groom.customer.domain.model.UserRole
import com.groom.customer.domain.port.LoadUserPort
import com.groom.customer.domain.port.VerifyPasswordPort
import com.groom.customer.domain.service.Authenticator
import com.groom.customer.domain.service.UserPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 일반 고객 인증 애플리케이션 서비스
 * 일반 고객의 로그인, 로그아웃 유스케이스를 담당합니다.
 */
@Service
class CustomerAuthenticationService(
    private val loadUserPort: LoadUserPort,
    private val verifyPasswordPort: VerifyPasswordPort,
    private val authenticator: Authenticator,
    private val userPolicy: UserPolicy,
) {
    /**
     * 일반 고객 로그인
     */
    @Transactional
    fun login(command: LoginCommand): LoginResult {
        throw IllegalStateException("쇼케이스를 위한 에러가 발생하였습니다.")

        // 1. 일반 고객 사용자 조회
        val user =
            loadUserPort.loadByEmailAndRole(command.email, UserRole.CUSTOMER)
                ?: throw AuthenticationException.UserNotFoundByEmail(email = command.email)

        // 2. 사용자 활성 상태 확인
        userPolicy.checkUserIsActive(user)

        // 3. 비밀번호 검증
        if (!verifyPasswordPort.verifyPassword(user, command.password)) {
            throw AuthenticationException.InvalidPassword(email = command.email)
        }

        // 4. 인증 정보 생성 및 저장 후 DTO로 변환
        return authenticator
            .createAndPersistCredentials(user, command.clientIp)
            .toLoginResult()
    }

    /**
     * 일반 고객 로그아웃
     */
    @Transactional
    fun logout(command: LogoutCommand) {
        // 1. 일반 고객 사용자 조회
        val user =
            loadUserPort.loadById(command.userId)
                ?: throw UserException.UserNotFound(userId = command.userId)

        // 2. 역할 확인 (도메인 정책 검증)
        userPolicy.checkUserHasRole(user, UserRole.CUSTOMER, "CustomerLogout")

        // 3. 인증 정보 무효화
        authenticator.revokeCredentials(user)
    }
}
