package com.groom.customer.adapter.outbound.security

import com.groom.customer.configuration.encryption.EncryptHashEncoderRegistry
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class BcryptHashConverter : AttributeConverter<String, String> {
    override fun convertToDatabaseColumn(attribute: String?): String? =
        attribute?.let { value ->
            if (value.isBcryptHash()) value else EncryptHashEncoderRegistry.encode(value)
        }

    override fun convertToEntityAttribute(dbData: String?): String? = dbData

    private fun String.isBcryptHash(): Boolean = BCRYPT_PATTERN.matches(this)

    companion object {
        private val BCRYPT_PATTERN =
            Regex("""^\$2[aby]\$\d{2}\$[./A-Za-z0-9]{53}$""")
    }
}
