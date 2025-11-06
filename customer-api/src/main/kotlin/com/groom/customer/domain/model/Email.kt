package com.groom.customer.domain.model

/**
 * 이메일 값 객체. 유효성 검증과 정규화를 책임진다.
 */
data class Email(
    val value: String,
) {
    init {
        require(EMAIL_REGEX.matches(value)) { "이메일 형식이 올바르지 않습니다." }
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

        fun from(raw: String): Email {
            val normalized = raw.trim().lowercase()
            require(normalized.isNotBlank()) { "이메일은 비어 있을 수 없습니다." }
            require(EMAIL_REGEX.matches(normalized)) { "이메일 형식이 올바르지 않습니다." }

            return Email(normalized)
        }
    }
}
