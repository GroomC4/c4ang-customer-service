package com.groom.customer.outbound.repository

import com.groom.customer.domain.model.UserAudit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserAuditRepositoryImpl : JpaRepository<UserAudit, UUID>
