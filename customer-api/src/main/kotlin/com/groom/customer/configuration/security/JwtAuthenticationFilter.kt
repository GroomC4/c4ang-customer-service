package com.groom.customer.configuration.security

import com.groom.customer.common.exception.TokenException
import com.groom.customer.security.jwt.JwtTokenProvider
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * JWT 인증 필터
 *
 * 모든 요청에 대해 한 번만 실행되며, Authorization 헤더에서 JWT 토큰을 추출하여 검증합니다.
 * 유효한 토큰인 경우 SecurityContext에 인증 정보를 설정합니다.
 */
@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
) : OncePerRequestFilter() {
    companion object {
        const val JWT_EXCEPTION_ATTRIBUTE = "jwt.exception"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            // 1. Authorization 헤더에서 JWT 토큰 추출
            val token = extractTokenFromRequest(request)

            // 2. 토큰이 있고 현재 인증되지 않은 상태라면
            if (token != null && SecurityContextHolder.getContext().authentication == null) {
                // 3. 토큰 검증 및 사용자 정보 추출
                val authData = jwtTokenProvider.validateToken(token)

                // 4. Spring Security 인증 객체 생성
                val authorities = listOf(SimpleGrantedAuthority("ROLE_${authData.roleName}"))
                val authentication =
                    UsernamePasswordAuthenticationToken(
                        authData.id, // principal: 사용자 ID
                        null, // credentials: 비밀번호 불필요
                        authorities, // authorities: 권한 목록
                    )

                // 5. 요청 세부 정보 설정
                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)

                // 6. SecurityContext에 인증 정보 설정
                SecurityContextHolder.getContext().authentication = authentication
            }
        } catch (e: TokenException) {
            // 토큰 관련 예외를 request attribute에 저장
            when (e) {
                is TokenException.TokenExpired -> logger.warn("JWT 토큰 만료: ${e.message}")
                is TokenException.InvalidTokenSignature -> logger.warn("JWT 토큰 서명 검증 실패: ${e.message}")
                is TokenException.InvalidTokenFormat -> logger.warn("JWT 토큰 형식 오류: ${e.message}")
                is TokenException.InvalidTokenAlgorithm -> logger.warn("JWT 토큰 알고리즘 오류: ${e.message}")
                is TokenException.InvalidTokenIssuer -> logger.warn("JWT 토큰 발급자 불일치: ${e.message}")
                is TokenException.MissingTokenClaim -> logger.warn("JWT 토큰 필수 클레임 누락: ${e.claimName}")
                is TokenException.MissingToken -> logger.warn("JWT 토큰 없음: ${e.message}")
            }
            request.setAttribute(JWT_EXCEPTION_ATTRIBUTE, e)
        } catch (e: Exception) {
            // 예상하지 못한 예외도 TokenException으로 변환하여 저장
            logger.warn("JWT 검증 중 예상하지 못한 오류: ${e.message}", e)
            request.setAttribute(JWT_EXCEPTION_ATTRIBUTE, TokenException.InvalidTokenFormat(cause = e))
        }

        // 7. 다음 필터로 요청 전달
        filterChain.doFilter(request, response)
    }

    /**
     * HTTP 요청에서 Bearer 토큰을 추출합니다.
     */
    private fun extractTokenFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7) // "Bearer " 제거
        } else {
            null
        }
    }
}
