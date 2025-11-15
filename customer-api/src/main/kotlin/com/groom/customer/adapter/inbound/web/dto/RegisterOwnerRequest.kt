package com.groom.customer.adapter.inbound.web.dto

import com.groom.customer.application.dto.RegisterOwnerCommand
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 판매자 회원가입 REST 입력 DTO.
 * 스토어 정보를 포함하여 판매자 계정과 스토어를 함께 생성한다.
 */
@Schema(description = "판매자 회원가입 요청")
data class RegisterOwnerRequest(
    @Schema(description = "사용자명", example = "김판매", required = true)
    val username: String,
    @Schema(description = "이메일 주소", example = "owner@example.com", required = true)
    val email: String,
    @Schema(description = "비밀번호", example = "password123!", required = true)
    val password: String,
    @Schema(description = "연락처", example = "010-9876-5432", required = true)
    val phoneNumber: String,
) {
    fun toCommand(): RegisterOwnerCommand =
        RegisterOwnerCommand(
            username = username,
            email = email,
            rawPassword = password,
            phoneNumber = phoneNumber,
        )
}
