package com.groom.customer.application.dto

import java.time.LocalDateTime

/**
 * 판매자 회원가입 유스케이스 출력 모델.
 * 생성된 사용자와 스토어 정보를 함께 반환한다.
 */
data class RegisterOwnerResult(
    val userId: String,
    val username: String,
    val email: String,
    val storeId: String,
    val storeName: String,
    val createdAt: LocalDateTime,
)
