package com.groom.customer.domain.model

import com.groom.customer.configuration.jpa.CreatedAndUpdatedAtAuditEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.DynamicUpdate
import java.util.UUID

@DynamicUpdate
@Entity
@Table(name = "p_user_profile")
class UserProfile(
    @Column(nullable = false)
    val fullName: String,
    @Column(nullable = false)
    val phoneNumber: String,
    @Column(nullable = false)
    val contactEmail: String,
    @Column
    val defaultAddress: String?,
) : CreatedAndUpdatedAtAuditEntity() {
    @Id
    @Column(columnDefinition = "uuid", updatable = false)
    var id: UUID = UUID.randomUUID()

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserProfile) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String =
        "UserProfileJpaEntity(id=$id, fullName=$fullName, phoneNumber=$phoneNumber, contactEmail=$contactEmail, defaultAddress=$defaultAddress)"
}
