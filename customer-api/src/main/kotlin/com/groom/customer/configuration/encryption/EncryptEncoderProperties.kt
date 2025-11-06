package com.groom.customer.configuration.encryption

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Encapsulates password encoder tuning so it can be adjusted via application.yml.
 */
@ConfigurationProperties(prefix = "encryption.encoder")
class EncryptEncoderProperties {
    var bcryptStrength: Int = 10
}
