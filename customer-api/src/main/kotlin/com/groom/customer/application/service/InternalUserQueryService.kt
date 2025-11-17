package com.groom.customer.application.service

import com.groom.customer.domain.model.User
import com.groom.customer.domain.port.LoadUserPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * K8s 내부 서비스 간 통신용 사용자 조회 서비스.
 * 다른 마이크로서비스에서 사용자 정보를 조회할 때 사용됩니다.
 */
@Service
@Transactional(readOnly = true)
class InternalUserQueryService(
    private val loadUserPort: LoadUserPort,
) {
    /**
     * 사용자 ID로 사용자 정보를 조회합니다.
     *
     * @param userId 조회할 사용자 ID
     * @return 사용자 도메인 객체 (존재하지 않으면 null)
     */
    fun getUserById(userId: UUID): User? = loadUserPort.loadById(userId)
}
