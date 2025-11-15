package com.groom.customer.adapter.inbound.web.dto

import com.groom.customer.application.dto.RegisterCustomerResult
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * REST 출력 DTO. 애플리케이션 계층의 결과를 외부 계약 형태로 직렬화한다.
 */
@Schema(description = "고객 회원가입 응답")
data class SignupCustomerResponse(
    @Schema(description = "사용자 ID", example = "123e4567-e89b-12d3-a456-426614174000")
    val userId: String,
    @Schema(description = "사용자명", example = "홍길동")
    val username: String,
    @Schema(description = "이메일 주소", example = "customer@example.com")
    val email: String,
    @Schema(description = "사용자 역할", example = "CUSTOMER")
    val role: String,
    @Schema(description = "활성화 상태", example = "true")
    val isActive: Boolean,
    @Schema(description = "생성일시", example = "2025-01-15T10:30:00")
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(result: RegisterCustomerResult): SignupCustomerResponse =
            SignupCustomerResponse(
                userId = result.userId,
                username = result.username,
                email = result.email,
                role = result.role.name,
                isActive = result.isActive,
                createdAt = result.createdAt,
            )
    }
}
