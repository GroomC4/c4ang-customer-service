package com.groom.customer.application.dto

/**
 * 고객 가입 유스케이스 입력 모델. 값 객체를 보유해 비즈니스 규칙을 보장한다.
 */
data class RegisterCustomerCommand(
    val username: String,
    val email: String,
    val rawPassword: String,
    val defaultAddress: String,
    val defaultPhoneNumber: String,
)
