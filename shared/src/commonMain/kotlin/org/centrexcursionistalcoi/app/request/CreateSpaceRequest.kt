package org.centrexcursionistalcoi.app.request

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.CategoryPrice
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.data.SpaceClimateControl
import org.centrexcursionistalcoi.app.data.SpaceEnergySource

@Serializable
data class CreateSpaceRequest(
    val name: String,
    val description: String,
    val conditionsOfUse: String? = null,
    val requiresKeys: Boolean = false,
    val prices: List<CategoryPrice> = emptyList(),
    val photos: List<FileWithContext> = emptyList(),
    val seats: Int? = null,
    val beds: Int? = null,
    val energySources: List<SpaceEnergySource> = emptyList(),
    val climateControl: List<SpaceClimateControl> = emptyList(),
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
) : RequestWithFiles<CreateSpaceRequest> {
    override fun mapFiles(transform: (FileWithContext) -> FileWithContext): CreateSpaceRequest = copy(photos = photos.map(transform))
}
