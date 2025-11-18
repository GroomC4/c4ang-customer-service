package com.groom.customer.adapter.inbound.web

import com.groom.customer.adapter.inbound.web.dto.UserInternalDto
import com.groom.customer.application.service.InternalUserService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * K8s 내부 사용자 조회 API
 *
 * 다른 마이크로서비스에서 사용자 정보를 조회하기 위한 Internal API
 * - API Gateway를 거치지 않고 K8s 내부에서만 호출 가능
 * - Istio ServiceEntry로 접근 제어
 * - Contract Test로 API 명세 검증
 */
@RestController
@RequestMapping("/internal/v1/users")
class InternalUserController(
    private val internalUserService: InternalUserService,
) {
    /**
     * 사용자 ID로 사용자 정보 조회
     *
     * @param userId 사용자 UUID
     * @return 사용자 정보 (UserInternalDto)
     */
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{userId}")
    fun getUserById(
        @PathVariable userId: String,
    ): UserInternalDto = internalUserService.getUserById(UUID.fromString(userId))
}
