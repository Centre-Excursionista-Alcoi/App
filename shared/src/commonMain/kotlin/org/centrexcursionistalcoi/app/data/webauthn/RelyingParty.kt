package org.centrexcursionistalcoi.app.data.webauthn

import kotlinx.serialization.Serializable

@Serializable
data class RelyingParty(
    val name: String,
    val id: String // e.g., "your.app.package.name" (must match your Android asset links or RP ID)
)
