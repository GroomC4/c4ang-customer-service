package com.groom.customer.domain.model

import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import java.time.LocalDateTime
import java.util.UUID

/**
 * UserAudit 엔티티.
 * DDL: p_user_audit 테이블
 *
 * 사용자 계정의 주요 변경 이력을 남기는 감사 로그.
 */
@Entity
@Table(name = "p_user_audit")
class UserAudit(
    @Column(nullable = false)
    val eventType: String, // USER_REGISTERED, PROFILE_UPDATED
    @Column
    val changeSummary: String? = null,
    @Column(nullable = false)
    val recordedAt: LocalDateTime = LocalDateTime.now(),
    @Type(JsonType::class)
    @Column(columnDefinition = "jsonb")
    val metadata: Map<String, Any>? = null,
) {
    @Id
    @Column(columnDefinition = "uuid", updatable = false)
    var id: UUID = UUID.randomUUID()

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserAudit) return false
        if (id == null || other.id == null) return false
        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: System.identityHashCode(this)

    override fun toString(): String = "UserAudit(id=$id, eventType=$eventType, recordedAt=$recordedAt)"
}
