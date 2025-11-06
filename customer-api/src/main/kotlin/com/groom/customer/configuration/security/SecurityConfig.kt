package com.groom.customer.configuration.security

import com.groom.customer.common.enums.UserRole
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

// 타입 별칭: 긴 Spring Security 타입 이름을 간결하게 사용
private typealias AuthRegistry = AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry

/**
 * Spring Security 설정
 *
 * JWT 기반 인증을 사용하며 Stateless 세션 정책을 적용합니다.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // @PreAuthorize 활성화
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val customAuthenticationEntryPoint: CustomAuthenticationEntryPoint,
    private val customAccessDeniedHandler: CustomAccessDeniedHandler,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            // CSRF 비활성화 (JWT 사용 시 불필요)
            .csrf { it.disable() }
            // 세션 관리: Stateless (JWT 사용)
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            // 인증/인가 실패 시 커스텀 에러 응답 반환
            .exceptionHandling { exceptions ->
                exceptions
                    .authenticationEntryPoint(customAuthenticationEntryPoint) // 401 Unauthorized
                    .accessDeniedHandler(customAccessDeniedHandler) // 403 Forbidden
            }
            // 인증/인가 규칙 설정
            .authorizeHttpRequests { auth ->
                auth
                    .forDevelopmentMatchers()
                    .userMatchers()
                    .storeMatchers()
                    .productOwnerMatchers()
                    .orderMatchers()
                    .paymentMatchers()
                    // 그 외 모든 요청은 인증 필요
                    .anyRequest()
                    .authenticated()
            }
            // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter::class.java,
            ).build()

    private fun AuthRegistry.forDevelopmentMatchers() =
        this
            .requestMatchers(
                "/actuator/health", // Spring Boot Actuator 헬스체크
                "/swagger-ui/**", // Swagger UI
                "/v3/api-docs/**", // OpenAPI 3.0 스펙
                "/swagger-resources/**", // Swagger 리소스
                "/webjars/**", // Swagger UI 정적 파일 (WebJars)
                "/api-docs/**", // API 문서 엔드포인트
            ).permitAll()

    private fun AuthRegistry.userMatchers() =
        this
            .requestMatchers(
                "/api/v1/auth/customers/signup",
                "/api/v1/auth/customers/login",
                "/api/v1/auth/owners/signup",
                "/api/v1/auth/owners/login",
                "/api/v1/auth/refresh", // 리프레시 토큰으로 인증하므로 permitAll
            ).permitAll()
            .requestMatchers("/api/v1/auth/customers/logout")
            .hasRole(UserRole.CUSTOMER.name)
            .requestMatchers("/api/v1/auth/owners/logout")
            .hasRole(UserRole.OWNER.name)

    private fun AuthRegistry.storeMatchers() =
        this
            .requestMatchers(HttpMethod.GET, "/api/v1/stores/**")
            .permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/stores/**")
            .hasRole(UserRole.OWNER.name)
            .requestMatchers(HttpMethod.PUT, "/api/v1/stores/**")
            .hasRole(UserRole.OWNER.name)
            .requestMatchers(HttpMethod.DELETE, "/api/v1/stores/**")
            .hasRole(UserRole.OWNER.name)

    private fun AuthRegistry.productOwnerMatchers() =
        this
            .requestMatchers("/api/v1/products/owner")
            .hasRole(UserRole.OWNER.name)
            .requestMatchers(HttpMethod.GET, "/api/v1/products/**")
            .hasAnyRole(UserRole.CUSTOMER.name, UserRole.OWNER.name) // 상품 조회는 일반고객/판매자만 가능
            .requestMatchers(HttpMethod.POST, "/api/v1/products/**")
            .hasRole(UserRole.OWNER.name)
            .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**")
            .hasRole(UserRole.OWNER.name)
            .requestMatchers(HttpMethod.PUT, "/api/v1/products/**")
            .hasRole(UserRole.OWNER.name)
            .requestMatchers(HttpMethod.PATCH, "/api/v1/products/**")
            .hasRole(UserRole.OWNER.name)

    private fun AuthRegistry.orderMatchers() =
        this
            .requestMatchers("/api/v1/orders/**")
            .hasRole(UserRole.CUSTOMER.name)

    private fun AuthRegistry.paymentMatchers() =
        this
            // PG 콜백 API: 외부 PG 시스템에서 호출 (인증 불필요)
            .requestMatchers("/external/pg/callback/payment/**")
            .permitAll()
            .requestMatchers("/api/v1/payments/**")
            .hasRole(UserRole.CUSTOMER.name)
}
