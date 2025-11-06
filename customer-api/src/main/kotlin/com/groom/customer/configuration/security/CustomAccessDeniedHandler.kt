package com.groom.customer.configuration.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.groom.customer.common.exception.ErrorCode
import com.groom.customer.common.exception.handler.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

/**
 * 인가 실패 시 커스텀 에러 응답을 반환하는 AccessDeniedHandler
 *
 * Spring Security의 기본 AccessDeniedHandler는 403 상태 코드만 반환하지만,
 * 이 커스텀 구현은 일관된 JSON 에러 응답 형식을 제공합니다.
 *
 * 인증은 성공했지만 권한이 부족한 경우 (예: CUSTOMER가 OWNER 전용 API 접근 시도)
 * 이 핸들러가 호출됩니다.
 */
@Component
class CustomAccessDeniedHandler(
    private val objectMapper: ObjectMapper,
) : AccessDeniedHandler {
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        // 권한 부족 시 403 Forbidden과 함께 JSON 에러 응답 반환
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"

        val errorResponse =
            ErrorResponse(
                code = ErrorCode.ACCESS_DENIED,
                message = "이 작업을 수행할 권한이 없습니다.",
            )
        val jsonResponse = objectMapper.writeValueAsString(errorResponse)

        response.writer.write(jsonResponse)
    }
}
