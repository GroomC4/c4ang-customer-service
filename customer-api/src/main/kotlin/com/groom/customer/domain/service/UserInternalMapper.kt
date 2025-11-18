package com.groom.customer.domain.service

import com.groom.customer.domain.model.User
import com.groom.customer.domain.model.UserRole
import com.groom.ecommerce.customer.api.avro.UserInternalResponse
import com.groom.ecommerce.customer.api.avro.UserProfileInternal
import com.groom.ecommerce.customer.api.avro.UserRole as AvroUserRole
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * User 도메인 모델을 Internal API용 Avro 스키마로 변환하는 도메인 서비스
 *
 * 비즈니스 로직:
 * - User 도메인 모델 → contract-hub Avro 스키마 변환
 * - 도메인 UserRole → contract Avro UserRole 매핑
 * - LocalDateTime → epoch millis 변환
 * - nullable 필드 처리 (profile이 없는 경우 기본값)
 */
@Service
class UserInternalMapper {
    /**
     * User 도메인 모델을 UserInternalResponse로 변환
     *
     * 매핑 규칙:
     * - CUSTOMER → CUSTOMER
     * - OWNER → OWNER
     * - MANAGER → ADMIN (contract-hub는 MANAGER를 지원하지 않음)
     * - MASTER → ADMIN (contract-hub는 MASTER를 지원하지 않음)
     *
     * @param user 변환할 User 도메인 모델
     * @return UserInternalResponse Avro 스키마
     */
    fun toUserInternalResponse(user: User): UserInternalResponse {
        val profileInternal = toUserProfileInternal(user)
        val userRole = mapToAvroUserRole(user.role)
        val createdAtMillis = toEpochMillis(user.createdAt)
        val updatedAtMillis = toEpochMillis(user.updatedAt)
        val lastLoginAtMillis = user.lastLoginAt?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

        return UserInternalResponse.newBuilder()
            .setUserId(user.id.toString())
            .setUsername(user.username)
            .setEmail(user.email)
            .setRole(userRole)
            .setIsActive(user.isActive)
            .setProfile(profileInternal)
            .setCreatedAt(createdAtMillis)
            .setUpdatedAt(updatedAtMillis)
            .setLastLoginAt(lastLoginAtMillis)
            .build()
    }

    /**
     * User의 profile을 UserProfileInternal로 변환
     *
     * @param user User 도메인 모델
     * @return UserProfileInternal (profile이 없으면 빈 값으로 채움)
     */
    private fun toUserProfileInternal(user: User): UserProfileInternal =
        UserProfileInternal
            .newBuilder()
            .setFullName(user.profile?.fullName ?: "")
            .setPhoneNumber(user.profile?.phoneNumber ?: "")
            .setAddress(null) // address는 User 엔티티에 없으므로 null
            .build()

    /**
     * 도메인 UserRole을 contract-hub Avro UserRole로 매핑
     *
     * 매핑 규칙:
     * - CUSTOMER → CUSTOMER
     * - OWNER → OWNER
     * - MANAGER → ADMIN (contract에서 MANAGER 미지원)
     * - MASTER → ADMIN (contract에서 MASTER 미지원)
     */
    private fun mapToAvroUserRole(role: UserRole): AvroUserRole =
        when (role) {
            UserRole.CUSTOMER -> AvroUserRole.CUSTOMER
            UserRole.OWNER -> AvroUserRole.OWNER
            UserRole.MANAGER -> AvroUserRole.ADMIN // MANAGER는 ADMIN으로 매핑
            UserRole.MASTER -> AvroUserRole.ADMIN // MASTER는 ADMIN으로 매핑
        }

    /**
     * LocalDateTime을 epoch milliseconds로 변환
     *
     * @param dateTime LocalDateTime (nullable)
     * @return epoch millis (null이면 0)
     */
    private fun toEpochMillis(dateTime: LocalDateTime?): Long =
        dateTime
            ?.atZone(ZoneId.systemDefault())
            ?.toInstant()
            ?.toEpochMilli() ?: 0L
}
