package org.centrexcursionistalcoi.app.data.webauthn

import kotlinx.serialization.Serializable

@Serializable
data class AuthenticatorSelection(
    // "platform" ensures the key is tied to the device (Google Password Manager / Restore Credentials)
    val authenticatorAttachment: String = "platform",
    val requireResidentKey: Boolean = true,
    val residentKey: String = "required"
)
