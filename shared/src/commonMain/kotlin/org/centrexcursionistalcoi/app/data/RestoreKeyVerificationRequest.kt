package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

@Serializable
data class RestoreKeyVerificationRequest(
    val authenticationResponseJson: String // The raw JSON string from Android
)
