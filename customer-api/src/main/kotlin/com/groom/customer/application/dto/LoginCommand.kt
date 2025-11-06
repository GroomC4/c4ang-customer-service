package com.groom.customer.application.dto

data class LoginCommand(
    val email: String,
    val password: String,
    val clientIp: String? = null,
)
