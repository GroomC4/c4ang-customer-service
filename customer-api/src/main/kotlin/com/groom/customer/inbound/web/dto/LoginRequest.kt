package com.groom.customer.inbound.web.dto

import com.groom.customer.application.dto.LoginCommand
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "로그인 요청")
data class LoginRequest(
    @Schema(description = "이메일 주소", example = "user@example.com", required = true)
    val email: String,
    @Schema(description = "비밀번호", example = "password123!", required = true)
    val password: String,
) {
    fun toCommand(clientIp: String? = null): LoginCommand =
        LoginCommand(
            email = email,
            password = password,
            clientIp = clientIp,
        )
}
