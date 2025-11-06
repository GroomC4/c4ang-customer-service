package com.groom.customer.domain.model

/**
 * 사용자 이름 값 객체. 한글 전용 이름과 길이 제한을 보장한다.
 */
data class Username(
    val value: String,
) {
    companion object {
        private const val MIN_LENGTH = 2
        private const val MAX_LENGTH = 10
        private val KOREAN_REGEX = Regex("^[가-힣]+$")
        private const val BLANK_MESSAGE = "사용자 이름은 비어 있을 수 없습니다."
        private const val LENGTH_MESSAGE = "사용자 이름은 2자 이상 10자 이하의 한글로 입력해주세요."
        private const val KOREAN_MESSAGE = "사용자 이름은 한글만 사용할 수 있습니다."

        fun from(raw: String): Username {
            val value = raw.trim()
            require(value.isNotBlank()) { BLANK_MESSAGE }
            require(value.length in MIN_LENGTH..MAX_LENGTH) { LENGTH_MESSAGE }
            require(KOREAN_REGEX.matches(value)) { KOREAN_MESSAGE }

            return Username(value)
        }
    }
}
