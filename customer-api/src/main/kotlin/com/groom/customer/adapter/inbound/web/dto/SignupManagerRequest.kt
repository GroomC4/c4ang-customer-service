package com.groom.customer.adapter.inbound.web.dto

import com.groom.customer.application.dto.RegisterManagerCommand
import io.swagger.v3.oas.annotations.media.Schema

/**
 * REST 입력 DTO. 외부 요청을 애플리케이션 계층에서 사용하는 커맨드로 변환한다.
 */
@Schema(description = "관리자 회원가입 요청")
data class SignupManagerRequest(
    @Schema(description = "사용자명", example = "관리자", required = true)
    val username: String,
    @Schema(description = "이메일 주소", example = "manager@example.com", required = true)
    val email: String,
    @Schema(description = "비밀번호", example = "password123!", required = true)
    val password: String,
    @Schema(description = "연락처", example = "010-1234-5678", required = true)
    val phoneNumber: String,
) {
    fun toCommand(): RegisterManagerCommand =
        RegisterManagerCommand(
            username = username,
            email = email,
            rawPassword = password,
            phoneNumber = phoneNumber,
        )
}
