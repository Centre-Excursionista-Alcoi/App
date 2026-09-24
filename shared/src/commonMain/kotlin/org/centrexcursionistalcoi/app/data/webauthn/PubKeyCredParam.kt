package org.centrexcursionistalcoi.app.data.webauthn

import kotlinx.serialization.Serializable

@Serializable
data class PubKeyCredParam(
    val type: String,
    val alg: Int
)
