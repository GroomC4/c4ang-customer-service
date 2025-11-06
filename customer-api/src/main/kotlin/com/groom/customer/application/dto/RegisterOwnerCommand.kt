package com.groom.customer.application.dto

/**
 * 판매자 회원가입 유스케이스 입력 모델.
 * 사용자 정보와 스토어 정보를 함께 받아 처리한다.
 */
data class RegisterOwnerCommand(
    val username: String,
    val email: String,
    val rawPassword: String,
    val phoneNumber: String,
    val storeName: String,
    val storeDescription: String?,
)
