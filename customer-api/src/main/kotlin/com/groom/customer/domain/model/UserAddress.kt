package com.groom.customer.domain.model

import com.groom.customer.configuration.jpa.CreatedAndUpdatedAtAuditEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime
import java.util.UUID

/**
 * UserAddress 엔티티.
 * DDL: p_user_address 테이블
 */
@Entity
@Table(
    name = "p_user_address",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["user_id", "label"]),
    ],
)
class UserAddress(
    @Column(nullable = false)
    val label: String, // 예: "집", "회사"
    @Column(nullable = false)
    val recipientName: String,
    @Column(nullable = false)
    val phoneNumber: String,
    @Column(nullable = false)
    val postalCode: String,
    @Column(nullable = false)
    val addressLine1: String,
    @Column
    val addressLine2: String? = null,
    @Column(nullable = false)
    val isDefault: Boolean = false,
    @Column
    val deletedAt: LocalDateTime? = null,
) : CreatedAndUpdatedAtAuditEntity() {
    @Id
    @Column(columnDefinition = "uuid", updatable = false)
    var id: UUID = UUID.randomUUID()

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserAddress) return false
        if (id == null || other.id == null) return false
        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: System.identityHashCode(this)

    override fun toString(): String = "UserAddress(id=$id, label=$label, isDefault=$isDefault)"
}
