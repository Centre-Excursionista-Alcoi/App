package org.centrexcursionistalcoi.app.data.manufacturer

import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.DrawableResource

@Serializable
sealed interface ManufacturerItemDetails {
    companion object {
        fun decode(value: String): ManufacturerItemDetails? {
            return when {
                PetzlItemDetails.SERIAL_REGEX.matches(value) -> PetzlItemDetails.parse(value)
                PetzlOldItemDetails.SERIAL_REGEX.matches(value) -> PetzlOldItemDetails.parse(value)
                else -> null
            }
        }
    }

    @Transient
    val logo: DrawableResource

    @Transient
    val name: String

    @Composable
    fun DataShowcase()
}
