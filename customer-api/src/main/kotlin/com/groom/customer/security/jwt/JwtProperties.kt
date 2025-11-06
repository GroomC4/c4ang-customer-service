package com.groom.customer.security.jwt

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties(prefix = "security.jwt")
data class JwtProperties
    @ConstructorBinding
    constructor(
        val secret: String,
        val issuer: String = "ecommerce-service-api",
        val accessTokenExpirationMinutes: Long = 5, // 5분
        val refreshTokenExpirationDays: Long = 3, // 3일
    ) {
        companion object {
            private const val MIN_SECRET_LENGTH = 32 // 최소 32자 (256비트)
        }

        init {
            // 비밀 키 존재 여부 검증
            require(secret.isNotBlank()) {
                "JWT secret must not be blank"
            }

            // 비밀 키 강도 검증 (약한 비밀 키 사용 방지)
            require(secret.length >= MIN_SECRET_LENGTH) {
                "JWT secret must be at least $MIN_SECRET_LENGTH characters long for security. " +
                    "Current length: ${secret.length}"
            }

            // 만료 시간 검증
            require(accessTokenExpirationMinutes > 0) {
                "JWT accessTokenExpirationMinutes must be positive"
            }
            require(refreshTokenExpirationDays > 0) {
                "JWT refreshTokenExpirationDays must be positive"
            }
        }
    }
