package com.groom.customer.application.service

import com.groom.customer.adapter.outbound.persistence.UserRepository
import com.groom.customer.common.exception.UserException
import com.groom.ecommerce.customer.api.avro.UserInternalResponse
import com.groom.ecommerce.customer.api.avro.UserProfileInternal
import com.groom.ecommerce.customer.api.avro.UserRole
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Internal User Service
 *
 * K8s 내부 마이크로서비스 간 통신을 위한 사용자 조회 서비스
 * - contract-hub의 Avro 스키마 기반 응답
 * - Read-only 트랜잭션으로 Replica DB 자동 라우팅
 */
@Service
@Transactional(readOnly = true)
class InternalUserService(
    private val userRepository: UserRepository,
) {
    /**
     * 사용자 ID로 사용자 정보 조회
     *
     * @param userId 사용자 UUID
     * @return UserInternalResponse (Avro 스키마)
     * @throws UserException.NotFound 사용자를 찾을 수 없는 경우
     */
    fun getUserById(userId: UUID): UserInternalResponse {
        val user =
            userRepository.findById(userId).orElseThrow {
                UserException.UserNotFound(userId)
            }

        // UserProfile 정보 변환
        val profileInternal =
            UserProfileInternal.newBuilder()
                .setFullName(user.profile?.fullName ?: "")
                .setPhoneNumber(user.profile?.phoneNumber ?: "")
                .setAddress(null) // address는 User 엔티티에 없으므로 null
                .build()

        // UserRole enum 변환 (contract-hub는 CUSTOMER, OWNER, ADMIN만 지원)
        val userRole =
            when (user.role) {
                com.groom.customer.domain.model.UserRole.CUSTOMER -> UserRole.CUSTOMER
                com.groom.customer.domain.model.UserRole.OWNER -> UserRole.OWNER
                com.groom.customer.domain.model.UserRole.MANAGER -> UserRole.ADMIN // MANAGER는 ADMIN으로 매핑
                com.groom.customer.domain.model.UserRole.MASTER -> UserRole.ADMIN // MASTER는 ADMIN으로 매핑
            }

        // LocalDateTime을 epoch millis로 변환
        val createdAtMillis = user.createdAt?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: 0L
        val updatedAtMillis = user.updatedAt?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: 0L
        val lastLoginAtMillis =
            user.lastLoginAt?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

        // UserInternalResponse 생성
        return UserInternalResponse.newBuilder()
            .setUserId(user.id.toString())
            .setUsername(user.username)
            .setEmail(user.email)
            .setRole(userRole)
            .setIsActive(user.isActive)
            .setProfile(profileInternal)
            .setCreatedAt(createdAtMillis)
            .setUpdatedAt(updatedAtMillis)
            .setLastLoginAt(lastLoginAtMillis)
            .build()
    }
}
