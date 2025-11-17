package com.groom.customer.domain.model

import com.groom.customer.adapter.outbound.security.BcryptHashConverter
import com.groom.customer.configuration.jpa.CreatedAndUpdatedAtAuditEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "p_user",
    uniqueConstraints = [
        jakarta.persistence.UniqueConstraint(columnNames = ["email", "role"]),
    ],
)
class User(
    @Column(nullable = false, unique = true, length = 10)
    val username: String,
    @Column(nullable = false)
    val email: String,
    @Column(nullable = false)
    @Convert(converter = BcryptHashConverter::class)
    val passwordHash: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: UserRole,
    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,
    @Column
    val lastLoginAt: LocalDateTime? = null,
    @Column
    val deletedAt: LocalDateTime? = null,
) : CreatedAndUpdatedAtAuditEntity() {
    @Id
    @Column(columnDefinition = "uuid", updatable = false)
    var id: UUID = UUID.randomUUID()

    @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    var profile: UserProfile? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false

        // id가 아직 할당되지 않은 경우 identity로 비교
        if (id == null || other.id == null) {
            return false
        }

        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: System.identityHashCode(this)

    override fun toString(): String =
        "UserJpaEntity(id=$id, username=$username, email=$email, role=$role, isActive=$isActive, lastLoginAt=$lastLoginAt, deletedAt=$deletedAt)"
}
