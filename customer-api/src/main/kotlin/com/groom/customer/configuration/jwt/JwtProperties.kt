package com.groom.customer.configuration.jwt

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties(prefix = "security.jwt")
data class JwtProperties
    @ConstructorBinding
    constructor(
        val privateKey: String,
        val publicKey: String,
        val keyId: String = "ecommerce-key-1",
        val issuer: String = "ecommerce-service-api",
        val accessTokenExpirationMinutes: Long = 5, // 5분
        val refreshTokenExpirationDays: Long = 3, // 3일
    ) {
        init {
            // RSA Private Key 존재 여부 검증
            require(privateKey.isNotBlank()) {
                "JWT privateKey must not be blank"
            }

            // RSA Public Key 존재 여부 검증
            require(publicKey.isNotBlank()) {
                "JWT publicKey must not be blank"
            }

            // PEM 형식 검증
            require(privateKey.contains("-----BEGIN") && privateKey.contains("PRIVATE KEY-----")) {
                "JWT privateKey must be in PEM format"
            }
            require(publicKey.contains("-----BEGIN") && publicKey.contains("PUBLIC KEY-----")) {
                "JWT publicKey must be in PEM format"
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
