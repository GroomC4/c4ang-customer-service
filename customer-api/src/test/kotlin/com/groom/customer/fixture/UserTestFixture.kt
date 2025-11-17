package com.groom.customer.fixture

import com.groom.customer.domain.model.UserRole
import com.groom.customer.domain.model.User
import com.groom.customer.domain.model.UserProfile
import java.time.LocalDateTime
import java.util.UUID

/**
 * User 엔티티 테스트 픽스처
 *
 * JPA Auditing으로 설정되는 createdAt/updatedAt 같은 필드를
 * 리플렉션으로 초기화하여 테스트용 User 객체를 생성합니다.
 */
object UserTestFixture {
    /**
     * 기본 User 생성
     */
    fun createUser(
        id: UUID = UUID.randomUUID(),
        username: String = "testuser",
        email: String = "test@example.com",
        passwordHash: String = "\$2a\$10\$dummyHashForTesting",
        role: UserRole = UserRole.CUSTOMER,
        isActive: Boolean = true,
        lastLoginAt: LocalDateTime? = null,
        deletedAt: LocalDateTime? = null,
        createdAt: LocalDateTime = LocalDateTime.now(),
        updatedAt: LocalDateTime = LocalDateTime.now(),
        profile: UserProfile? = null,
    ): User {
        val user =
            User(
                username = username,
                email = email,
                passwordHash = passwordHash,
                role = role,
                isActive = isActive,
                lastLoginAt = lastLoginAt,
                deletedAt = deletedAt,
            )

        // 리플렉션으로 protected 필드 설정
        setField(user, "id", id)
        setField(user, "createdAt", createdAt)
        setField(user, "updatedAt", updatedAt)

        if (profile != null) {
            user.profile = profile
        }

        return user
    }

    /**
     * CUSTOMER 역할 User 생성
     */
    fun createCustomer(
        username: String = "customer",
        email: String = "customer@example.com",
    ): User =
        createUser(
            username = username,
            email = email,
            role = UserRole.CUSTOMER,
        )

    /**
     * OWNER 역할 User 생성
     */
    fun createOwner(
        username: String = "owner",
        email: String = "owner@example.com",
    ): User =
        createUser(
            username = username,
            email = email,
            role = UserRole.OWNER,
        )

    /**
     * 기본 UserProfile 생성
     */
    fun createUserProfile(
        id: UUID = UUID.randomUUID(),
        fullName: String = "Test User",
        phoneNumber: String = "010-1234-5678",
        contactEmail: String = "contact@example.com",
        defaultAddress: String? = null,
        createdAt: LocalDateTime = LocalDateTime.now(),
        updatedAt: LocalDateTime = LocalDateTime.now(),
    ): UserProfile {
        val profile =
            UserProfile(
                fullName = fullName,
                phoneNumber = phoneNumber,
                contactEmail = contactEmail,
                defaultAddress = defaultAddress,
            )

        // 리플렉션으로 protected 필드 설정
        setField(profile, "id", id)
        setField(profile, "createdAt", createdAt)
        setField(profile, "updatedAt", updatedAt)

        return profile
    }

    /**
     * 리플렉션으로 필드 설정 (private/protected 필드 접근)
     */
    fun setField(
        target: Any,
        fieldName: String,
        value: Any?,
    ) {
        var clazz: Class<*>? = target.javaClass
        while (clazz != null) {
            try {
                val field = clazz.getDeclaredField(fieldName)
                field.isAccessible = true
                field.set(target, value)
                return
            } catch (e: NoSuchFieldException) {
                clazz = clazz.superclass
            }
        }
        throw NoSuchFieldException("Field $fieldName not found in ${target.javaClass}")
    }
}
