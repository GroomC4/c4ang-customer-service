package com.groom.customer.adapter.inbound.web

import com.groom.customer.adapter.inbound.web.dto.RefreshTokenRequest
import com.groom.customer.adapter.inbound.web.dto.RefreshTokenResponse
import com.groom.customer.application.service.RefreshTokenService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Token Management", description = "토큰 관리 API")
@RestController
@RequestMapping("/api/v1/auth")
class TokenRefreshController(
    private val refreshTokenService: RefreshTokenService,
) {
    @Operation(summary = "액세스 토큰 갱신", description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "토큰 갱신 성공",
            ),
            ApiResponse(
                responseCode = "401",
                description = "유효하지 않은 리프레시 토큰",
            ),
        ],
    )
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/refresh")
    fun refresh(
        @RequestBody request: RefreshTokenRequest,
    ): RefreshTokenResponse {
        val result = refreshTokenService.refresh(request.toCommand())
        return RefreshTokenResponse.from(result)
    }
}
