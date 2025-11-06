package com.groom.customer.inbound.web.dto

import com.groom.customer.application.dto.RegisterOwnerCommand
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 판매자 회원가입 REST 입력 DTO.
 * 스토어 정보를 포함하여 판매자 계정과 스토어를 함께 생성한다.
 */
@Schema(description = "판매자 회원가입 요청")
data class RegisterOwnerRequest(
    @Schema(description = "사용자명", example = "김판매", required = true)
    val username: String,
    @Schema(description = "이메일 주소", example = "owner@example.com", required = true)
    val email: String,
    @Schema(description = "비밀번호", example = "password123!", required = true)
    val password: String,
    @Schema(description = "연락처", example = "010-9876-5432", required = true)
    val phoneNumber: String,
    @Schema(description = "스토어 정보", required = true)
    val storeInfo: StoreInfo,
) {
    @Schema(description = "스토어 정보")
    data class StoreInfo(
        @Schema(description = "스토어명", example = "김판매의 스토어", required = true)
        val name: String,
        @Schema(description = "스토어 설명", example = "최고의 상품을 판매하는 스토어입니다", required = false)
        val description: String?,
    )

    fun toCommand(): RegisterOwnerCommand =
        RegisterOwnerCommand(
            username = username,
            email = email,
            rawPassword = password,
            phoneNumber = phoneNumber,
            storeName = storeInfo.name,
            storeDescription = storeInfo.description,
        )
}
