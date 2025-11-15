package com.groom.customer.adapter.inbound.web.dto

import com.groom.customer.application.dto.LogoutCommand
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "로그아웃 요청")
data class LogoutRequest(
    @Schema(description = "사용자 ID", example = "123e4567-e89b-12d3-a456-426614174000", required = true)
    val userId: UUID,
) {
    fun toCommand(): LogoutCommand =
        LogoutCommand(
            userId = userId,
        )
}
