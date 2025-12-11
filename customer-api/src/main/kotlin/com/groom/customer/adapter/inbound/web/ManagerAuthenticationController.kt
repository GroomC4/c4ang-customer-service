package com.groom.customer.adapter.inbound.web

import com.groom.customer.adapter.inbound.web.dto.LoginRequest
import com.groom.customer.adapter.inbound.web.dto.LoginResponse
import com.groom.customer.adapter.inbound.web.dto.SignupManagerRequest
import com.groom.customer.adapter.inbound.web.dto.SignupManagerResponse
import com.groom.customer.application.dto.LogoutCommand
import com.groom.customer.application.service.ManagerAuthenticationService
import com.groom.customer.application.service.RegisterManagerService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "Manager Authentication", description = "관리자 인증 관리 API")
@RestController
@RequestMapping("/api/v1/auth/managers")
class ManagerAuthenticationController(
    private val managerAuthenticationService: ManagerAuthenticationService,
    private val registerManagerService: RegisterManagerService,
) {
    @Operation(summary = "관리자 회원가입", description = "새로운 관리자 계정을 생성합니다. (테스트/개발 환경용)")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "회원가입 성공",
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
            ),
            ApiResponse(
                responseCode = "409",
                description = "이미 존재하는 이메일",
            ),
        ],
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/signup")
    fun registerManager(
        @RequestBody request: SignupManagerRequest,
    ): SignupManagerResponse {
        val result = registerManagerService.register(request.toCommand())
        return SignupManagerResponse.from(result)
    }

    @Operation(summary = "관리자 로그인", description = "관리자 계정으로 로그인하고 액세스 토큰과 리프레시 토큰을 발급받습니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "로그인 성공",
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패 (이메일 또는 비밀번호 오류)",
            ),
        ],
    )
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest,
        httpRequest: HttpServletRequest,
    ): LoginResponse {
        val clientIp = httpRequest.remoteAddr
        val result = managerAuthenticationService.login(request.toCommand(clientIp))
        return LoginResponse.from(result)
    }

    @Operation(summary = "관리자 로그아웃", description = "현재 로그인된 관리자 계정에서 로그아웃합니다. (MANAGER 권한 필요)")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "로그아웃 성공"),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패",
            ),
            ApiResponse(
                responseCode = "403",
                description = "권한 없음",
            ),
        ],
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/logout")
    fun logout(
        @RequestHeader("X-User-Id") userIdHeader: String,
    ) {
        // Istio가 JWT 검증 후 주입한 헤더에서 사용자 ID 추출
        val userId = UUID.fromString(userIdHeader)
        managerAuthenticationService.logout(LogoutCommand(userId))
    }
}
