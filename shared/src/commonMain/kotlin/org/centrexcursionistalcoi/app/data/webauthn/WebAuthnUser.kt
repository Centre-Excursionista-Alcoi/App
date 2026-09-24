package org.centrexcursionistalcoi.app.data.webauthn

import kotlinx.serialization.Serializable

@Serializable
data class WebAuthnUser(
    val id: String,          // Base64Url encoded byte array of the user's DB ID
    val name: String,        // e.g., "user@example.com"
    val displayName: String  // e.g., "John Doe"
)
