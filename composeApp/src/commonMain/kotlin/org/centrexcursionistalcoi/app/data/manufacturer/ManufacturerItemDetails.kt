package org.centrexcursionistalcoi.app.data.manufacturer

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.DrawableResource

@Serializable
sealed interface ManufacturerItemDetails {
    companion object {
        fun decode(value: String): ManufacturerItemDetails? {
            return when {
                PetzlItemDetails.SERIAL_REGEX.matches(value) -> PetzlItemDetails.parse(value)
                else -> null
            }
        }
    }

    @Transient
    val logo: DrawableResource

    @Transient
    val name: String
}
