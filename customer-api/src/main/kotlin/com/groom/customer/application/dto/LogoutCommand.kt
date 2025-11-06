package com.groom.customer.application.dto

import java.util.UUID

data class LogoutCommand(
    val userId: UUID,
)
