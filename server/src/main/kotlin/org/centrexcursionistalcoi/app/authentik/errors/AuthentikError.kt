package org.centrexcursionistalcoi.app.authentik.errors

import kotlinx.serialization.Serializable

@Serializable(AuthentikErrorSerializer::class)
sealed interface AuthentikError {
    val code: String

    fun asThrowable(): AuthentikException = AuthentikException(this)
}
