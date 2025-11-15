package com.groom.customer.adapter.inbound.web.dto

import com.groom.customer.application.dto.LoginResult
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "로그인 응답")
data class LoginResponse(
    @Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    val accessToken: String,
    @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    val refreshToken: String,
    @Schema(description = "토큰 만료 시간 (초)", example = "3600")
    val expiresIn: Long,
    @Schema(description = "토큰 타입", example = "Bearer")
    val tokenType: String,
) {
    companion object {
        fun from(result: LoginResult): LoginResponse =
            LoginResponse(
                accessToken = result.accessToken,
                refreshToken = result.refreshToken,
                expiresIn = result.expiresIn,
                tokenType = result.tokenType,
            )
    }
}
