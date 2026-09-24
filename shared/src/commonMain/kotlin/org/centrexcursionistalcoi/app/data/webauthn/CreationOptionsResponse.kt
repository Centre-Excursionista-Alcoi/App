package org.centrexcursionistalcoi.app.data.webauthn

import kotlinx.serialization.Serializable

@Serializable
data class CreationOptionsResponse(
    val challenge: String, // Base64Url encoded string (no padding)
    val rp: RelyingParty,
    val user: WebAuthnUser,
    val pubKeyCredParams: List<PubKeyCredParam> = listOf(
        // ES256 (Standard Elliptic Curve)
        PubKeyCredParam(type = "public-key", alg = -7),
        // RS256 (Standard RSA as fallback)
        PubKeyCredParam(type = "public-key", alg = -257)
    ),
    val authenticatorSelection: AuthenticatorSelection = AuthenticatorSelection(),
    val timeout: Long = 1800000 // 30 minutes in milliseconds
)
