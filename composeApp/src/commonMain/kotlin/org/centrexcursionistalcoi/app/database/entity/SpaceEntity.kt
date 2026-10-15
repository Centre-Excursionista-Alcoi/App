package org.centrexcursionistalcoi.app.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import org.centrexcursionistalcoi.app.data.CategoryPrice
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.data.Space
import org.centrexcursionistalcoi.app.data.SpaceClimateControl
import org.centrexcursionistalcoi.app.data.SpaceEnergySource
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity(tableName = "Spaces")
data class SpaceEntity(
    @PrimaryKey
    val id: Uuid,
    val name: String,
    val description: String,
    val conditionsOfUse: String?,
    val requiresKeys: Boolean,
    val prices: List<CategoryPrice>,
    val isClosed: Boolean,
    val closedSince: Instant?,
    val closedUntil: Instant?,
    val closedReason: String?,
    val photos: List<FileWithContext>,
    val seats: Int?,
    val beds: Int?,
    val energySources: List<SpaceEnergySource>,
    val climateControl: List<SpaceClimateControl>,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val altitude: Double?,
) {
    fun toSpace() = Space(
        id = id,
        name = name,
        description = description,
        conditionsOfUse = conditionsOfUse,
        requiresKeys = requiresKeys,
        prices = prices,
        isClosed = isClosed,
        closedSince = closedSince,
        closedUntil = closedUntil,
        closedReason = closedReason,
        photos = photos,
        seats = seats,
        beds = beds,
        energySources = energySources,
        climateControl = climateControl,
        address = address,
        latitude = latitude,
        longitude = longitude,
        altitude = altitude,
    )

    companion object {
        fun Space.toEntity() = SpaceEntity(
            id = id,
            name = name,
            description = description,
            conditionsOfUse = conditionsOfUse,
            requiresKeys = requiresKeys,
            prices = prices,
            isClosed = isClosed,
            closedSince = closedSince,
            closedUntil = closedUntil,
            closedReason = closedReason,
            photos = photos,
            seats = seats,
            beds = beds,
            energySources = energySources,
            climateControl = climateControl,
            address = address,
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
        )
    }
}
