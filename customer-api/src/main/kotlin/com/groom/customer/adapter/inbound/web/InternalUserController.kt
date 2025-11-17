package com.groom.customer.adapter.inbound.web

import com.groom.customer.adapter.inbound.web.dto.internal.UserInternalResponse
import com.groom.customer.application.service.InternalUserQueryService
import com.groom.customer.common.exception.UserException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * K8s 내부 서비스 간 통신용 사용자 조회 API 컨트롤러.
 * 외부 노출되지 않고 클러스터 내부 서비스들이 사용합니다.
 */
@RestController
@RequestMapping("/internal/v1/users")
@Tag(name = "Internal User API", description = "K8s 내부 서비스 간 사용자 정보 조회 API")
class InternalUserController(
    private val internalUserQueryService: InternalUserQueryService,
) {
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "사용자 ID로 조회", description = "사용자 ID로 사용자 정보를 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        ],
    )
    @GetMapping("/{userId}")
    fun getUserById(
        @Parameter(description = "사용자 ID", required = true)
        @PathVariable userId: UUID,
    ): UserInternalResponse {
        val user =
            internalUserQueryService.getUserById(userId)
                ?: throw UserException.UserNotFound(userId)

        return UserInternalResponse.from(user)
    }
}
