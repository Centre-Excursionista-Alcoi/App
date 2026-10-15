package org.centrexcursionistalcoi.app.viewmodel.management

import io.github.vinceglb.filekit.PlatformFile
import org.centrexcursionistalcoi.app.data.CategoryPrice
import org.centrexcursionistalcoi.app.data.SpaceClimateControl
import org.centrexcursionistalcoi.app.data.SpaceEnergySource
import kotlin.uuid.Uuid

/**
 * What the form to create or edit a space holds. Unset optional values are `null`: when editing, they clear what the
 * space had.
 */
data class SpaceDraft(
    val name: String,
    val description: String,
    val conditionsOfUse: String,
    val requiresKeys: Boolean,
    val prices: List<CategoryPrice>,
    val seats: Int?,
    val beds: Int?,
    val energySources: List<SpaceEnergySource>,
    val climateControl: List<SpaceClimateControl>,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val altitude: Double?,
    /** Photos to add. */
    val newPhotos: List<PlatformFile> = emptyList(),
    /** Ids of the photos the space has that are to be removed. */
    val removedPhotos: List<Uuid> = emptyList(),
)
