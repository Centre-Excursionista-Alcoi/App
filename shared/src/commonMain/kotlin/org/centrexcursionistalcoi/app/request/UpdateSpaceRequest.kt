package org.centrexcursionistalcoi.app.request

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.CategoryPrice
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.data.Space
import org.centrexcursionistalcoi.app.data.SpaceClimateControl
import org.centrexcursionistalcoi.app.data.SpaceDetail
import org.centrexcursionistalcoi.app.data.SpaceEnergySource
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Nullable fields are left as they are when null. To clear [conditionsOfUse], [closedReason] or [address] send an empty
 * string; to clear the other optional values list them in [clear]. [energySources] and [climateControl] replace the
 * whole list. Opening the space (`isClosed = false`) clears the closed fields.
 *
 * [photos] works like a post's files: a file with contents is added, one with only an `id` is removed.
 */
@Serializable
data class UpdateSpaceRequest(
    val name: String? = null,
    val description: String? = null,
    val conditionsOfUse: String? = null,

    val prices: List<CategoryPrice>? = null,
    val requiresKeys: Boolean? = null,

    val isClosed: Boolean? = null,
    val closedSince: Instant? = null,
    val closedUntil: Instant? = null,
    val closedReason: String? = null,

    val photos: List<FileWithContext>? = null,
    val seats: Int? = null,
    val beds: Int? = null,
    val energySources: List<SpaceEnergySource>? = null,
    val climateControl: List<SpaceClimateControl>? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val clear: Set<SpaceDetail> = emptySet(),
): UpdateEntityRequest<Uuid, Space>, RequestWithFiles<UpdateSpaceRequest> {
    override fun mapFiles(transform: (FileWithContext) -> FileWithContext): UpdateSpaceRequest = copy(photos = photos?.map(transform))

    override fun isEmpty(): Boolean = name == null &&
            description == null &&
            conditionsOfUse == null &&
            prices == null &&
            requiresKeys == null &&
            isClosed == null &&
            closedSince == null &&
            closedUntil == null &&
            closedReason == null &&
            photos.isNullOrEmpty() &&
            seats == null &&
            beds == null &&
            energySources == null &&
            climateControl == null &&
            address == null &&
            latitude == null &&
            longitude == null &&
            altitude == null &&
            clear.isEmpty()
}
