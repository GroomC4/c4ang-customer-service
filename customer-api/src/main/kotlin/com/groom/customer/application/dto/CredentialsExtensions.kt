package com.groom.customer.application.dto

import com.groom.customer.domain.model.TokenCredentials

/**
 * TokenCredentials를 LoginResult로 변환합니다.
 * JWT 특화 정보(tokenType)는 여기서 추가됩니다.
 */
fun TokenCredentials.toLoginResult(): LoginResult =
    LoginResult(
        accessToken = this.primaryToken,
        refreshToken = this.secondaryToken ?: "",
        expiresIn = this.getValiditySeconds(),
        tokenType = "Bearer",
    )

/**
 * TokenCredentials를 RefreshTokenResult로 변환합니다.
 * JWT 특화 정보(tokenType)는 여기서 추가됩니다.
 */
fun TokenCredentials.toRefreshTokenResult(): RefreshTokenResult =
    RefreshTokenResult(
        accessToken = this.primaryToken,
        expiresIn = this.getValiditySeconds(),
        tokenType = "Bearer",
    )
