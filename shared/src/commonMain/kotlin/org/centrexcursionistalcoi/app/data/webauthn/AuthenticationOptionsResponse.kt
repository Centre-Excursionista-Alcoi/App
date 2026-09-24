package org.centrexcursionistalcoi.app.data.webauthn

import kotlinx.serialization.Serializable

/**
 * The WebAuthn "get" (authentication) ceremony's options, matching the `PublicKeyCredentialRequestOptionsJSON`
 * shape -- distinct from [CreationOptionsResponse] (the "create"/registration shape): there's no `user` or
 * `pubKeyCredParams` here, since the server doesn't know in advance which credential/user will respond.
 *
 * [allowCredentials] is deliberately omitted/empty: this is a discoverable-credential (resident key) request --
 * the platform surfaces whichever matching credential(s) it already has, rather than the server having to name
 * one upfront. That's what makes both Restore Credentials (the server has no session yet to know who's asking)
 * and passkey login work the same way.
 */
@Serializable
data class AuthenticationOptionsResponse(
    val challenge: String, // Base64Url encoded string (no padding)
    val rpId: String,
    val timeout: Long = 1800000, // 30 minutes in milliseconds
    val userVerification: String = "preferred",
)
