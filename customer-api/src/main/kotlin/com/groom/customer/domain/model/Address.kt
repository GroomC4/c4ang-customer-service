package com.groom.customer.domain.model

/**
 * 주소 값 객체. 공백을 정리하고 비어 있지 않은지 검증한다.
 */
data class Address(
    val value: String,
) {
    companion object {
        fun from(raw: String): Address {
            val normalized = raw.trim()
            require(normalized.isNotEmpty()) { "기본 주소는 비어 있을 수 없습니다." }
            return Address(normalized)
        }
    }
}
