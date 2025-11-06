package com.groom.customer.inbound.web

import com.groom.customer.application.dto.LogoutCommand
import com.groom.customer.application.service.CustomerAuthenticationService
import com.groom.customer.application.service.RegisterCustomerService
import com.groom.customer.inbound.web.dto.LoginRequest
import com.groom.customer.inbound.web.dto.LoginResponse
import com.groom.customer.inbound.web.dto.SignupCustomerRequest
import com.groom.customer.inbound.web.dto.SignupCustomerResponse
import com.groom.customer.security.AuthenticationContext
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Customer Authentication", description = "고객 인증 관리 API")
@RestController
@RequestMapping("/api/v1/auth/customers")
class CustomerAuthenticationController(
    private val customerAuthenticationService: CustomerAuthenticationService,
    private val registerCustomerService: RegisterCustomerService,
    private val authenticationContext: AuthenticationContext,
) {
    @Operation(summary = "고객 회원가입", description = "새로운 고객 계정을 생성합니다.")
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
    fun registerCustomer(
        @RequestBody request: SignupCustomerRequest,
    ): SignupCustomerResponse {
        val result = registerCustomerService.register(request.toCommand())
        return SignupCustomerResponse.from(result)
    }

    @Operation(summary = "고객 로그인", description = "고객 계정으로 로그인하고 액세스 토큰과 리프레시 토큰을 발급받습니다.")
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
        val result = customerAuthenticationService.login(request.toCommand(clientIp))
        return LoginResponse.from(result)
    }

    @Operation(summary = "고객 로그아웃", description = "현재 로그인된 고객 계정에서 로그아웃합니다. (CUSTOMER 권한 필요)")
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
    @PreAuthorize("hasRole('CUSTOMER')") // CUSTOMER 역할만 접근 가능
    fun logout() {
        // JWT 토큰에서 사용자 ID 추출
        val userId = authenticationContext.getCurrentUserId()
        customerAuthenticationService.logout(LogoutCommand(userId))
    }
}
