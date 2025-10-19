package org.centrexcursionistalcoi.app.authentik.errors

import kotlinx.serialization.Serializable

@Serializable
data class Authentik403Error(
    override val code: String,
    val detail: String,
): AuthentikError
