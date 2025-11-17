package com.groom.customer.domain.service

import com.groom.customer.common.exception.AuthenticationException
import com.groom.customer.common.exception.PermissionException
import com.groom.customer.common.exception.UserException
import com.groom.customer.domain.model.User
import com.groom.customer.domain.model.UserRole
import com.groom.customer.domain.port.LoadUserPort
import com.groom.customer.domain.port.VerifyPasswordPort
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserPolicy(
    private val loadUserPort: LoadUserPort,
    private val verifyPasswordPort: VerifyPasswordPort,
) {
    /**
     * 이메일로 활성화된 고객 사용자를 조회합니다.
     *
     * @param email 사용자 이메일
     * @return 활성화된 고객 사용자
     * @throws AuthenticationException.UserNotFoundByEmail 사용자를 찾을 수 없거나 비활성화된 경우
     */
    fun loadActiveCustomerByEmail(email: String): User {
        val user =
            loadUserPort.loadByEmailAndRole(email, UserRole.CUSTOMER)
                ?: throw AuthenticationException.UserNotFoundByEmail(email = email)

        if (!user.isActive) {
            throw AuthenticationException.UserNotFoundByEmail(email = email)
        }

        return user
    }

    /**
     * 이메일로 활성화된 판매자 사용자를 조회합니다.
     *
     * @param email 사용자 이메일
     * @return 활성화된 판매자 사용자
     * @throws AuthenticationException.UserNotFoundByEmail 사용자를 찾을 수 없거나 비활성화된 경우
     */
    fun loadActiveOwnerByEmail(email: String): User {
        val user =
            loadUserPort.loadByEmailAndRole(email, UserRole.OWNER)
                ?: throw AuthenticationException.UserNotFoundByEmail(email = email)

        if (!user.isActive) {
            throw AuthenticationException.UserNotFoundByEmail(email = email)
        }

        return user
    }

    /**
     * 사용자 ID로 고객을 조회하고 역할을 검증합니다.
     *
     * @param userId 사용자 ID
     * @return 고객 사용자
     * @throws UserException.UserNotFound 사용자를 찾을 수 없는 경우
     * @throws PermissionException.AccessDenied 고객 역할이 아닌 경우
     */
    fun loadCustomerById(userId: UUID): User {
        val user =
            loadUserPort.loadById(userId)
                ?: throw UserException.UserNotFound(userId = userId)

        checkUserHasRole(user, UserRole.CUSTOMER, "CustomerLogout")
        return user
    }

    /**
     * 사용자 ID로 판매자를 조회하고 역할을 검증합니다.
     *
     * @param userId 사용자 ID
     * @return 판매자 사용자
     * @throws UserException.UserNotFound 사용자를 찾을 수 없는 경우
     * @throws PermissionException.AccessDenied 판매자 역할이 아닌 경우
     */
    fun loadOwnerById(userId: UUID): User {
        val user =
            loadUserPort.loadById(userId)
                ?: throw UserException.UserNotFound(userId = userId)

        checkUserHasRole(user, UserRole.OWNER, "OwnerLogout")
        return user
    }

    /**
     * 비밀번호를 검증합니다.
     *
     * @param user 검증할 사용자
     * @param password 입력된 비밀번호
     * @param email 이메일 (예외 메시지용)
     * @throws AuthenticationException.InvalidPassword 비밀번호가 일치하지 않는 경우
     */
    fun validatePassword(
        user: User,
        password: String,
        email: String,
    ) {
        if (!verifyPasswordPort.verifyPassword(user, password)) {
            throw AuthenticationException.InvalidPassword(email = email)
        }
    }

    fun checkAlreadyRegister(
        email: String,
        role: UserRole,
    ) {
        val existingUser = loadUserPort.loadByEmailAndRole(email, role)
        if (existingUser != null) {
            throw UserException.DuplicateEmail(email = email)
        }
    }

    /**
     * 사용자가 OWNER 역할을 가지고 있는지 DB 기반으로 확인한다.
     *
     * @param userId 사용자 UUID
     * @throws UserException.UserNotFound 사용자를 찾을 수 없는 경우
     * @throws UserException.InsufficientPermission OWNER 역할이 아닌 경우
     */
    fun checkOwnerRole(userId: UUID) {
        val user =
            loadUserPort.loadById(userId)
                ?: throw UserException.UserNotFound(userId)

        if (user.role != UserRole.OWNER) {
            throw UserException.InsufficientPermission(
                userId = userId,
                requiredRole = UserRole.OWNER.name,
                currentRole = user.role.name,
            )
        }
    }

    /**
     * 사용자가 특정 역할을 가지고 있는지 확인한다.
     * 주로 로그아웃이나 역할 기반 접근 제어에 사용된다.
     *
     * @param user 검증할 사용자
     * @param expectedRole 기대되는 역할
     * @param resource 접근하려는 리소스 이름 (예외 메시지용)
     * @throws PermissionException.AccessDenied 역할이 일치하지 않는 경우
     */
    fun checkUserHasRole(
        user: User,
        expectedRole: UserRole,
        resource: String,
    ) {
        if (user.role != expectedRole) {
            throw PermissionException.AccessDenied(
                resource = resource,
                userId = user.id,
            )
        }
    }
}
