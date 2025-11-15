package com.groom.customer.adapter.inbound.web.dto

import com.groom.customer.application.dto.RefreshTokenCommand
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "토큰 갱신 요청")
data class RefreshTokenRequest(
    @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", required = true)
    val refreshToken: String,
) {
    fun toCommand(): RefreshTokenCommand =
        RefreshTokenCommand(
            refreshToken = refreshToken,
        )
}
