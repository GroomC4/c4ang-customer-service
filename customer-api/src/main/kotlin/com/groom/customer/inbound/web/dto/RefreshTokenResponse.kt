package com.groom.customer.inbound.web.dto

import com.groom.customer.application.dto.RefreshTokenResult
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "토큰 갱신 응답")
data class RefreshTokenResponse(
    @Schema(description = "새로운 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    val accessToken: String,
    @Schema(description = "토큰 만료 시간 (초)", example = "3600")
    val expiresIn: Long,
    @Schema(description = "토큰 타입", example = "Bearer")
    val tokenType: String,
) {
    companion object {
        fun from(result: RefreshTokenResult): RefreshTokenResponse =
            RefreshTokenResponse(
                accessToken = result.accessToken,
                expiresIn = result.expiresIn,
                tokenType = result.tokenType,
            )
    }
}
