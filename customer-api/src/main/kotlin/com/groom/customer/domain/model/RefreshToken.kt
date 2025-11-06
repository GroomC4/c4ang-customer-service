package com.groom.customer.domain.model

import com.groom.customer.configuration.jpa.CreatedAndUpdatedAtAuditEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "p_user_refresh_token")
class RefreshToken(
    @Column(nullable = false, unique = true, name = "user_id")
    val userId: UUID,
    @Column(nullable = true)
    var token: String?,
    @Column(name = "client_ip")
    val clientIp: String?,
    @Column(nullable = false, name = "expires_at")
    var expiresAt: LocalDateTime,
) : CreatedAndUpdatedAtAuditEntity() {
    @Id
    @Column(columnDefinition = "uuid", updatable = false)
    var id: UUID = UUID.randomUUID()

    /**
     * Refresh Token을 갱신합니다.
     */
    fun updateToken(
        newToken: String,
        newExpiresAt: LocalDateTime,
    ) {
        this.token = newToken
        this.expiresAt = newExpiresAt
    }

    /**
     * 토큰을 무효화합니다 (로그아웃 시 사용).
     */
    fun invalidate() {
        this.token = null
    }

    /**
     * 토큰이 만료되었는지 확인합니다.
     */
    fun isExpired(now: LocalDateTime): Boolean = now.isAfter(expiresAt)

    /**
     * 토큰이 유효한지 확인합니다.
     */
    fun isValid(now: LocalDateTime): Boolean = token != null && !isExpired(now)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RefreshToken) return false

        if (id == null || other.id == null) {
            return false
        }

        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: System.identityHashCode(this)

    override fun toString(): String = "RefreshToken(id=$id, userId=$userId, token=${token?.take(10)}..., expiresAt=$expiresAt)"
}
