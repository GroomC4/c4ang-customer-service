package com.groom.customer.adapter.inbound.web

import com.groom.customer.adapter.outbound.security.jwt.JwtTokenProvider
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * JWKS (JSON Web Key Set) 엔드포인트
 *
 * Istio RequestAuthentication이 JWT를 검증할 때 공개키를 가져오는 엔드포인트입니다.
 * RSA 공개키만 노출하며, 비밀키는 customer-api 내부에서만 사용됩니다.
 */
@RestController
class JwksController(
    private val jwtTokenProvider: JwtTokenProvider,
) {
    private val jwkSet: JWKSet by lazy {
        val rsaKey = RSAKey.Builder(jwtTokenProvider.publicKey)
            .keyID(jwtTokenProvider.keyId)
            .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
            .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
            .build()

        JWKSet(rsaKey)
    }

    @GetMapping(
        path = ["/.well-known/jwks.json"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getJwks(): Map<String, Any> {
        return jwkSet.toJSONObject()
    }
}
