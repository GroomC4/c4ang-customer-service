package com.groom.customer.configuration.encryption

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
@EnableConfigurationProperties(EncryptEncoderProperties::class)
class EncryptHashEncoderConfig(
    private val encryptEncoderProperties: EncryptEncoderProperties,
) {
    @Bean
    fun encryptHashEncoder(): PasswordEncoder = BCryptPasswordEncoder(encryptEncoderProperties.bcryptStrength)
}
