package com.groom.customer.application.dto

data class LoginResult(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long, // Access Token 만료 시간 (초 단위)
    val tokenType: String = "Bearer",
)
