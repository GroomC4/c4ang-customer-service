package com.groom.customer.configuration.encryption

import jakarta.annotation.PostConstruct
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicReference

@Component
class EncryptHashEncoderRegistry(
    private val encryptHashEncoder: PasswordEncoder,
) {
    @PostConstruct
    fun registerEncoder() {
        setPasswordEncoder(encryptHashEncoder)
    }

    fun encode(raw: String): String = encryptHashEncoder.encode(raw)

    fun matches(
        raw: String,
        encoded: String,
    ): Boolean = encryptHashEncoder.matches(raw, encoded)

    companion object {
        private val encoderRef = AtomicReference<PasswordEncoder>()

        internal fun setPasswordEncoder(encryptHashEncoder: PasswordEncoder) {
            encoderRef.set(encryptHashEncoder)
        }

        fun encode(raw: String): String = encoder().encode(raw)

        fun matches(
            raw: String,
            encoded: String,
        ): Boolean = encoder().matches(raw, encoded)

        private fun encoder(): PasswordEncoder = requireNotNull(encoderRef.get()) { "PasswordEncoder bean has not been initialised" }
    }
}
