package com.groom.customer.domain.service

import com.groom.customer.adapter.inbound.web.dto.UserInternalDto
import com.groom.customer.adapter.inbound.web.dto.UserProfileDto
import com.groom.customer.domain.model.User
import org.springframework.stereotype.Service
import java.time.ZoneId

/**
 * Internal User Domain Service
 *
 * User 도메인 모델과 Internal API 응답 DTO 간 변환을 담당하는 도메인 서비스
 *
 * 책임:
 * - User 도메인 모델 → UserInternalDto 변환
 * - LocalDateTime → Epoch milliseconds 변환
 * - 도메인 로직이 포함되지 않은 순수 매핑 로직
 */
@Service
class UserInternalMapper {
    /**
     * User 도메인 모델을 UserInternalDto로 변환
     *
     * @param user User 도메인 모델
     * @return UserInternalDto (Internal API 응답 DTO)
     */
    fun toUserInternalDto(user: User): UserInternalDto {
        val profile =
            user.profile
                ?: throw IllegalStateException("User profile must not be null for internal API")

        val profileDto =
            UserProfileDto(
                fullName = profile.fullName,
                phoneNumber = profile.phoneNumber,
                address = null, // UserAddress는 별도 엔티티이므로 일단 null
            )

        val createdAtMillis = user.createdAt?.let { toEpochMillis(it) } ?: 0L
        val updatedAtMillis = user.updatedAt?.let { toEpochMillis(it) } ?: 0L
        val lastLoginAtMillis =
            user.lastLoginAt
                ?.atZone(ZoneId.systemDefault())
                ?.toInstant()
                ?.toEpochMilli()

        return UserInternalDto(
            userId = user.id.toString(),
            username = user.username,
            email = user.email,
            role = user.role.name,
            isActive = user.isActive,
            profile = profileDto,
            createdAt = createdAtMillis,
            updatedAt = updatedAtMillis,
            lastLoginAt = lastLoginAtMillis,
        )
    }

    /**
     * LocalDateTime을 Epoch milliseconds로 변환
     */
    private fun toEpochMillis(dateTime: java.time.LocalDateTime): Long = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
