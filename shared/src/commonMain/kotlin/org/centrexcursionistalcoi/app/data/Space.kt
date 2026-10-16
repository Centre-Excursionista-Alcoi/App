package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.InstantSerializer
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class Space(
    override val id: Uuid,
    val name: String,
    val description: String,
    val conditionsOfUse: String?,

    val requiresKeys: Boolean,

    val prices: List<CategoryPrice>,

    /** Photos of the inside of the space. Their contents are downloaded from `/download/{id}`. */
    val photos: List<FileWithContext> = emptyList(),

    /** How many people can sit (live/eat) in the space at once. */
    val seats: Int? = null,
    /** How many people can sleep in the space. */
    val beds: Int? = null,
    val energySources: List<SpaceEnergySource> = emptyList(),
    val climateControl: List<SpaceClimateControl> = emptyList(),

    val address: String? = null,
    /** With [longitude]: either both are set, or none. In degrees. */
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** Metres above sea level. */
    val altitude: Double? = null,

    val isClosed: Boolean,
    @Serializable(InstantSerializer::class) val closedSince: Instant?,
    @Serializable(InstantSerializer::class) val closedUntil: Instant?,
    val closedReason: String?,
): Entity<Uuid>, ImageFileListContainer {
    override val images: List<Uuid> get() = photos.mapNotNull { it.id }
}
