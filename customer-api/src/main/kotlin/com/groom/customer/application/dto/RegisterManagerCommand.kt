package com.groom.customer.application.dto

/**
 * 관리자 가입 유스케이스 입력 모델. 테스트/개발 환경용.
 */
data class RegisterManagerCommand(
    val username: String,
    val email: String,
    val rawPassword: String,
    val phoneNumber: String,
)
