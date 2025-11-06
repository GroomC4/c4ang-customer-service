package com.groom.customer.configuration.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.groom.customer.common.exception.AuthenticationException
import com.groom.customer.common.exception.ErrorCode
import com.groom.customer.common.exception.TokenException
import com.groom.customer.common.exception.handler.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import org.springframework.security.core.AuthenticationException as SpringAuthenticationException

/**
 * 인증 실패 시 커스텀 에러 응답을 반환하는 AuthenticationEntryPoint
 *
 * Spring Security의 기본 AuthenticationEntryPoint는 401 상태 코드만 반환하지만,
 * 이 커스텀 구현은 일관된 JSON 에러 응답 형식을 제공합니다.
 *
 * JwtAuthenticationFilter에서 저장한 예외 정보를 확인하여 적절한 에러 메시지를 반환합니다.
 */
@Component
class CustomAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: SpringAuthenticationException,
    ) {
        // JwtAuthenticationFilter에서 저장한 예외 확인
        val jwtException = request.getAttribute(JwtAuthenticationFilter.JWT_EXCEPTION_ATTRIBUTE)

        // 예외 타입에 따라 적절한 에러 코드와 메시지 결정
        val (errorCode, errorMessage) =
            when (jwtException) {
                is TokenException.TokenExpired -> ErrorCode.TOKEN_EXPIRED to jwtException.message
                is TokenException.InvalidTokenSignature -> ErrorCode.INVALID_TOKEN_SIGNATURE to jwtException.message
                is TokenException.InvalidTokenFormat -> ErrorCode.INVALID_TOKEN_FORMAT to jwtException.message
                is TokenException.InvalidTokenAlgorithm -> ErrorCode.INVALID_TOKEN_ALGORITHM to jwtException.message
                is TokenException.InvalidTokenIssuer -> ErrorCode.INVALID_TOKEN_ISSUER to jwtException.message
                is TokenException.MissingTokenClaim -> ErrorCode.MISSING_TOKEN_CLAIM to jwtException.message
                is TokenException.MissingToken -> ErrorCode.MISSING_TOKEN to jwtException.message
                is AuthenticationException.UserNotFoundByEmail -> ErrorCode.USER_NOT_FOUND_BY_EMAIL to jwtException.message
                is AuthenticationException.InvalidPassword -> ErrorCode.INVALID_PASSWORD to jwtException.message
                is AuthenticationException.InvalidCredentials -> ErrorCode.INVALID_CREDENTIALS to jwtException.message
                else -> ErrorCode.MISSING_TOKEN to "인증에 실패하였습니다."
            }

        // 인증 실패 시 401 Unauthorized와 함께 JSON 에러 응답 반환
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"

        val errorResponse = ErrorResponse(code = errorCode, message = errorMessage ?: "인증에 실패하였습니다.")
        val jsonResponse = objectMapper.writeValueAsString(errorResponse)

        response.writer.write(jsonResponse)
    }
}
