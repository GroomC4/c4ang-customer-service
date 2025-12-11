package com.groom.customer.domain.service

import com.groom.customer.domain.model.Address
import com.groom.customer.domain.model.Email
import com.groom.customer.domain.model.PhoneNumber
import com.groom.customer.domain.model.User
import com.groom.customer.domain.model.UserProfile
import com.groom.customer.domain.model.UserRole
import com.groom.customer.domain.model.Username
import org.springframework.stereotype.Component

/**
 * User 애그리게이트의 복잡한 생성 로직을 담당하는 팩토리.
 * User와 UserProfile의 일관성 있는 생성과 양방향 관계 설정을 보장한다.
 */
@Component
class UserFactory {
    /**
     * 새로운 고객 사용자를 생성한다.
     * User와 UserProfile을 함께 생성하고 양방향 관계를 설정한다.
     *
     * @param username 사용자 이름 (2-10자 한글)
     * @param email 이메일 주소
     * @param passwordHash 해시된 비밀번호
     * @param defaultAddress 기본 주소
     * @param defaultPhoneNumber 전화번호 (010-1234-5678 형식)
     * @return 생성된 User 엔티티 (UserProfile이 포함됨)
     */
    fun createNewCustomer(
        username: Username,
        email: Email,
        passwordHash: String,
        defaultAddress: Address,
        defaultPhoneNumber: PhoneNumber,
    ): User {
        val user =
            User(
                username = username.value,
                email = email.value,
                passwordHash = passwordHash,
                role = UserRole.CUSTOMER,
            )

        val profile =
            UserProfile(
                fullName = username.value,
                phoneNumber = defaultPhoneNumber.value,
                contactEmail = email.value,
                defaultAddress = defaultAddress.value,
            ).apply {
                this.user = user
            }

        return user.apply { this.profile = profile }
    }

    /**
     * 새로운 판매자 사용자를 생성한다.
     * User와 UserProfile을 함께 생성하고 양방향 관계를 설정한다.
     *
     * @param username 사용자 이름 (2-10자 한글)
     * @param email 이메일 주소
     * @param passwordHash 해시된 비밀번호
     * @param phoneNumber 전화번호 (010-1234-5678 형식)
     * @return 생성된 User 엔티티 (UserProfile이 포함됨)
     */
    fun createNewOwner(
        username: Username,
        email: Email,
        passwordHash: String,
        phoneNumber: PhoneNumber,
    ): User {
        val user =
            User(
                username = username.value,
                email = email.value,
                passwordHash = passwordHash,
                role = UserRole.OWNER,
            )

        val profile =
            UserProfile(
                fullName = username.value,
                phoneNumber = phoneNumber.value,
                contactEmail = email.value,
                defaultAddress = null,
            ).apply {
                this.user = user
            }

        return user.apply { this.profile = profile }
    }

    /**
     * 새로운 관리자 사용자를 생성한다.
     * User와 UserProfile을 함께 생성하고 양방향 관계를 설정한다.
     * 테스트/개발 환경용.
     *
     * @param username 사용자 이름 (2-10자 한글)
     * @param email 이메일 주소
     * @param passwordHash 해시된 비밀번호
     * @param phoneNumber 전화번호 (010-1234-5678 형식)
     * @return 생성된 User 엔티티 (UserProfile이 포함됨)
     */
    fun createNewManager(
        username: Username,
        email: Email,
        passwordHash: String,
        phoneNumber: PhoneNumber,
    ): User {
        val user =
            User(
                username = username.value,
                email = email.value,
                passwordHash = passwordHash,
                role = UserRole.MANAGER,
            )

        val profile =
            UserProfile(
                fullName = username.value,
                phoneNumber = phoneNumber.value,
                contactEmail = email.value,
                defaultAddress = null,
            ).apply {
                this.user = user
            }

        return user.apply { this.profile = profile }
    }
}
