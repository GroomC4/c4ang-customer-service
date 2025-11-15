package com.groom.customer.adapter.inbound.web.dto

import com.groom.customer.application.dto.RegisterOwnerResult
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * 판매자 회원가입 REST 출력 DTO.
 * 사용자 정보와 생성된 스토어 정보를 함께 반환한다.
 */
@Schema(description = "판매자 회원가입 응답")
data class RegisterOwnerResponse(
    @Schema(description = "사용자 정보")
    val user: UserInfo,
    @Schema(description = "생성일시", example = "2025-01-15T10:30:00")
    val createdAt: LocalDateTime,
) {
    @Schema(description = "사용자 정보")
    data class UserInfo(
        @Schema(description = "사용자 ID", example = "123e4567-e89b-12d3-a456-426614174000")
        val id: String,
        @Schema(description = "사용자명", example = "김판매")
        val name: String,
        @Schema(description = "이메일 주소", example = "owner@example.com")
        val email: String,
    )

    companion object {
        fun from(result: RegisterOwnerResult): RegisterOwnerResponse =
            RegisterOwnerResponse(
                user =
                    UserInfo(
                        id = result.userId,
                        name = result.username,
                        email = result.email,
                    ),
                createdAt = result.createdAt,
            )
    }
}
