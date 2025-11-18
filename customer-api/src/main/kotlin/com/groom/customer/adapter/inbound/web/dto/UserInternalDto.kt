package com.groom.customer.adapter.inbound.web.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Internal User API 응답 DTO
 *
 * K8s 내부 마이크로서비스 간 통신을 위한 사용자 정보 응답
 * contract-hub의 UserInternalResponse Avro 스키마와 동일한 구조
 */
@Schema(description = "Internal User API 응답")
data class UserInternalDto(
    @Schema(description = "사용자 UUID", example = "750e8400-e29b-41d4-a716-446655440001")
    @JsonProperty("userId")
    val userId: String,
    @Schema(description = "사용자명", example = "홍길동")
    @JsonProperty("username")
    val username: String,
    @Schema(description = "이메일", example = "user@example.com")
    @JsonProperty("email")
    val email: String,
    @Schema(description = "역할 (CUSTOMER, OWNER, ADMIN)", example = "CUSTOMER")
    @JsonProperty("role")
    val role: String,
    @Schema(description = "활성 상태", example = "true")
    @JsonProperty("isActive")
    val isActive: Boolean,
    @Schema(description = "프로필 정보")
    @JsonProperty("profile")
    val profile: UserProfileDto,
    @Schema(description = "생성 시각 (epoch milliseconds)", example = "1699999999999")
    @JsonProperty("createdAt")
    val createdAt: Long,
    @Schema(description = "수정 시각 (epoch milliseconds)", example = "1699999999999")
    @JsonProperty("updatedAt")
    val updatedAt: Long,
    @Schema(description = "마지막 로그인 시각 (epoch milliseconds, nullable)", example = "1699999999999")
    @JsonProperty("lastLoginAt")
    val lastLoginAt: Long?,
)

@Schema(description = "사용자 프로필 정보")
data class UserProfileDto(
    @Schema(description = "전체 이름", example = "홍길동")
    @JsonProperty("fullName")
    val fullName: String,
    @Schema(description = "전화번호", example = "010-1234-5678")
    @JsonProperty("phoneNumber")
    val phoneNumber: String,
    @Schema(description = "주소 (nullable)", example = "서울시 강남구")
    @JsonProperty("address")
    val address: String?,
)
