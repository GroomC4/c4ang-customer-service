package com.groom.customer.application.dto

/**
 * 판매자 회원가입 유스케이스 입력 모델.
 */
data class RegisterOwnerCommand(
    val username: String,
    val email: String,
    val rawPassword: String,
    val phoneNumber: String,
)
