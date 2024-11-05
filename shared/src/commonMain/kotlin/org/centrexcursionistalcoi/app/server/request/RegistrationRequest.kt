package org.centrexcursionistalcoi.app.server.request

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.validation.isValidNif

@Serializable
data class RegistrationRequest(
    val name: String,
    val familyName: String,
    val nif: String,
    val phone: String
) {
    fun validate(): Boolean {
        if (name.isBlank()) return false
        if (familyName.isBlank()) return false
        if (!nif.isValidNif) return false
        if (phone.isBlank()) return false

        return true
    }
}
