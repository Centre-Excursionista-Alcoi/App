package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRestoreKeyRequest(
    val registrationResponseJson: String // The raw JSON string from Android
)
