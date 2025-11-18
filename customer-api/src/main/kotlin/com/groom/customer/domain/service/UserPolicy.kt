package com.groom.customer.domain.service

import com.groom.customer.common.exception.PermissionException
import com.groom.customer.common.exception.UserException
import com.groom.customer.domain.model.User
import com.groom.customer.domain.model.UserRole
import com.groom.customer.domain.port.LoadUserPort
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserPolicy(
    private val loadUserPort: LoadUserPort,
) {
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

    /**
     * 사용자가 활성 상태인지 확인한다.
     * 비활성화된 사용자는 로그인할 수 없다.
     *
     * @param user 검증할 사용자
     * @throws com.groom.customer.common.exception.AuthenticationException.UserNotFoundByEmail 사용자가 비활성화된 경우
     */
    fun checkUserIsActive(user: User) {
        if (!user.isActive) {
            throw com.groom.customer.common.exception.AuthenticationException.UserNotFoundByEmail(user.email)
        }
    }
}
