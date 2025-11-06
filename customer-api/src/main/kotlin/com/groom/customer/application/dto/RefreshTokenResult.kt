package com.groom.customer.application.dto

data class RefreshTokenResult(
    val accessToken: String,
    val expiresIn: Long, // 5분 = 300초
    val tokenType: String = "Bearer",
)
