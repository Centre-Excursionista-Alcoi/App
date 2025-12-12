package org.centrexcursionistalcoi.app.data.manufacturer

import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.petzl
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.DrawableResource

@Serializable
sealed class BasePetzlItemDetails : ManufacturerItemDetails {
    @Transient
    override val logo: DrawableResource = Res.drawable.petzl

    @Transient
    override val name: String = "Petzl"
}