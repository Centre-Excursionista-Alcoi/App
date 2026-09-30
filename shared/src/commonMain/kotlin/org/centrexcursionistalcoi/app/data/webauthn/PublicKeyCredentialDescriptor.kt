package org.centrexcursionistalcoi.app.data.webauthn

import kotlinx.serialization.Serializable

@Serializable
data class PublicKeyCredentialDescriptor(
    /** Base64Url encoded (no padding) credential id. */
    val id: String,
    val type: String = "public-key",
)
