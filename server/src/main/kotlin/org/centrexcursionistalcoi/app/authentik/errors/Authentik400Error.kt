package org.centrexcursionistalcoi.app.authentik.errors

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Authentik400Error(
    override val code: String,
    @SerialName("non_field_errors") val nonFieldErrors: List<String>,
): AuthentikError
