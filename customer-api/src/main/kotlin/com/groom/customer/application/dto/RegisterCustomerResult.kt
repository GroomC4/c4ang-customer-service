package com.groom.customer.application.dto

import com.groom.customer.domain.model.UserRole
import java.time.LocalDateTime

/**
 * 고객 가입 유스케이스 출력 모델. 컨트롤러에서 REST 응답으로 변환된다.
 */
data class RegisterCustomerResult(
    val userId: String,
    val username: String,
    val email: String,
    val role: UserRole,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
)
