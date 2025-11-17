package com.groom.customer.adapter.inbound.web.dto.internal

import com.groom.customer.domain.model.User
import com.groom.customer.domain.model.UserRole
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

/**
 * K8s 내부 서비스 간 통신용 User 응답 DTO.
 * 외부 노출이 아닌 내부 서비스 전용으로 민감한 정보는 제외됩니다.
 */
@Schema(description = "사용자 정보 조회 응답 (Internal API)")
data class UserInternalResponse(
    @Schema(description = "사용자 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val id: UUID,
    @Schema(description = "사용자명", example = "홍길동")
    val username: String,
    @Schema(description = "이메일", example = "hong@example.com")
    val email: String,
    @Schema(description = "역할", example = "CUSTOMER")
    val role: UserRole,
    @Schema(description = "활성화 여부", example = "true")
    val isActive: Boolean,
    @Schema(description = "프로필 정보")
    val profile: UserProfileInternal?,
    @Schema(description = "마지막 로그인 시간")
    val lastLoginAt: LocalDateTime?,
    @Schema(description = "생성 시간")
    val createdAt: LocalDateTime,
    @Schema(description = "수정 시간")
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(user: User): UserInternalResponse =
            UserInternalResponse(
                id = user.id,
                username = user.username,
                email = user.email,
                role = user.role,
                isActive = user.isActive,
                profile =
                    user.profile?.let {
                        UserProfileInternal(
                            fullName = it.fullName,
                            phoneNumber = it.phoneNumber,
                            defaultAddress = it.defaultAddress,
                        )
                    },
                lastLoginAt = user.lastLoginAt,
                createdAt = user.createdAt!!,
                updatedAt = user.updatedAt!!,
            )
    }
}

@Schema(description = "사용자 프로필 정보")
data class UserProfileInternal(
    @Schema(description = "전체 이름", example = "홍길동")
    val fullName: String,
    @Schema(description = "전화번호", example = "010-1234-5678")
    val phoneNumber: String,
    @Schema(description = "기본 주소", example = "서울시 강남구")
    val defaultAddress: String?,
)
