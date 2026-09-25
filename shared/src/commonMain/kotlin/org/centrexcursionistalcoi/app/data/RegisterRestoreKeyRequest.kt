package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRestoreKeyRequest(
    val registrationResponseJson: String, // The raw JSON string from Android
    /**
     * The credential ID (Base64Url, no padding) of the restore key this one replaces on the same device, if any.
     * Android keeps a single restore key per app, so the previous one can never be redeemed again and its
     * server-side record is deleted once this one is registered.
     */
    val replacesCredentialId: String? = null,
)
