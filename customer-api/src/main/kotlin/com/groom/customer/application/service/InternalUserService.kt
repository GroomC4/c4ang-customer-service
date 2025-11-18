package com.groom.customer.application.service

import com.groom.customer.adapter.inbound.web.dto.UserInternalDto
import com.groom.customer.common.exception.UserException
import com.groom.customer.domain.port.LoadUserPort
import com.groom.customer.domain.service.UserInternalMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Internal User Application Service
 *
 * K8s 내부 마이크로서비스 간 통신을 위한 사용자 조회 애플리케이션 서비스
 *
 * 책임:
 * - Use Case 조율 (사용자 조회 → DTO 응답 변환)
 * - Port를 통한 User 조회 (Infrastructure 계층에 의존하지 않음)
 * - Domain Service를 통한 변환 로직 위임
 * - Read-only 트랜잭션으로 Replica DB 자동 라우팅
 *
 * 헥사고날 아키텍처:
 * - Inbound Port: InternalUserController가 이 서비스를 호출
 * - Outbound Port: LoadUserPort를 통해 User 조회
 * - Domain Service: UserInternalMapper를 통해 도메인 모델 → DTO 변환
 */
@Service
@Transactional(readOnly = true)
class InternalUserService(
    private val loadUserPort: LoadUserPort,
    private val userInternalMapper: UserInternalMapper,
) {
    /**
     * 사용자 ID로 사용자 정보 조회 (Use Case)
     *
     * 흐름:
     * 1. Port를 통해 User 도메인 모델 조회
     * 2. Domain Service를 통해 DTO로 변환
     *
     * @param userId 사용자 UUID
     * @return UserInternalDto (Internal API 응답 DTO)
     * @throws UserException.UserNotFound 사용자를 찾을 수 없는 경우
     */
    fun getUserById(userId: UUID): UserInternalDto {
        // 1. Port를 통해 User 도메인 모델 조회
        val user =
            loadUserPort.loadById(userId)
                ?: throw UserException.UserNotFound(userId)

        // 2. Domain Service를 통해 DTO로 변환
        return userInternalMapper.toUserInternalDto(user)
    }
}
