package com.groom.customer.domain.model

/**
 * 전화번호 값 객체. 숫자만 허용하고 표준 포맷(010-1234-5678)으로 변환한다.
 */
data class PhoneNumber(
    val value: String,
) {
    companion object {
        private val HYPHENATED_REGEX = Regex("^01[0-9]-[0-9]{3,4}-[0-9]{4}$")

        fun from(raw: String): PhoneNumber {
            require(raw.isNotBlank()) { "전화번호는 비어 있을 수 없습니다." }
            require(HYPHENATED_REGEX.matches(raw)) { "전화번호는 010-1234-5678 형식이어야 합니다." }
            return PhoneNumber(raw)
        }
    }
}
