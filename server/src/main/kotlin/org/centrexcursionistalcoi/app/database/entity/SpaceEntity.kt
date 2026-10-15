package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.data.CategoryPrice
import org.centrexcursionistalcoi.app.data.Space
import org.centrexcursionistalcoi.app.data.SpaceClimateControl
import org.centrexcursionistalcoi.app.data.SpaceDetail
import org.centrexcursionistalcoi.app.data.SpaceEnergySource
import org.centrexcursionistalcoi.app.database.table.SpaceImages
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.routes.PatchRejectedException
import org.centrexcursionistalcoi.app.utils.validateSpaceDetails
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.base.EntityPatcher
import org.centrexcursionistalcoi.app.database.entity.base.LastUpdateEntity
import org.centrexcursionistalcoi.app.database.table.Spaces
import org.centrexcursionistalcoi.app.now
import org.centrexcursionistalcoi.app.request.UpdateSpaceRequest
import org.centrexcursionistalcoi.app.routes.helper.notifyUpdateForEntity
import org.centrexcursionistalcoi.app.utils.takeUnlessEmpty
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import kotlin.time.Instant
import kotlin.uuid.Uuid

class SpaceEntity(id: EntityID<Uuid>): UuidEntity(id), LastUpdateEntity, EntityDataConverter<Space, Uuid>, EntityPatcher<UpdateSpaceRequest> {
    override var lastUpdate: Instant by Spaces.lastUpdate

    var name: String by Spaces.name
    var description: String by Spaces.description
    var conditionsOfUse: String? by Spaces.conditionsOfUse
    var prices: List<CategoryPrice> by Spaces.prices

    var requiresKeys by Spaces.requiresKeys

    var seats: Int? by Spaces.seats
    var beds: Int? by Spaces.beds
    var energySources: List<SpaceEnergySource> by Spaces.energySources
    var climateControl: List<SpaceClimateControl> by Spaces.climateControl
    var address: String? by Spaces.address
    var latitude: Double? by Spaces.latitude
    var longitude: Double? by Spaces.longitude
    var altitude: Double? by Spaces.altitude

    val photos by FileEntity via SpaceImages

    var isClosed by Spaces.isClosed
    var closedSince by Spaces.closedSince
    var closedUntil by Spaces.closedUntil
    var closedReason by Spaces.closedReason

    override suspend fun updated() {
        notifyUpdateForEntity(Companion, id)
        Database { lastUpdate = now() }
    }

    context(_: JdbcTransaction)
    override fun toData(): Space = Space(
        id = id.value,
        name = name,
        description = description,
        conditionsOfUse = conditionsOfUse,
        prices = prices,
        requiresKeys = requiresKeys,
        isClosed = isClosed,
        closedSince = closedSince,
        closedUntil = closedUntil,
        closedReason = closedReason,
        photos = photos.map { it.toData() },
        seats = seats,
        beds = beds,
        energySources = energySources,
        climateControl = climateControl,
        address = address,
        latitude = latitude,
        longitude = longitude,
        altitude = altitude,
    )

    context(_: JdbcTransaction)
    override fun patch(request: UpdateSpaceRequest) {
        request.name?.takeUnlessEmpty()?.let { name = it }
        request.description?.takeUnlessEmpty()?.let { description = it }
        // An empty string clears them
        request.conditionsOfUse?.let { conditionsOfUse = it.takeUnlessEmpty() }
        request.prices?.let { prices = it }
        request.requiresKeys?.let { requiresKeys = it }
        request.seats?.let { seats = it }
        request.beds?.let { beds = it }
        request.altitude?.let { altitude = it }
        request.address?.let { address = it.trim().takeUnless { value -> value.isEmpty() } }
        request.energySources?.let { energySources = it.distinct() }
        request.climateControl?.let { climateControl = it.distinct() }
        if (request.latitude != null || request.longitude != null) {
            latitude = request.latitude ?: latitude
            longitude = request.longitude ?: longitude
        }
        request.clear.forEach { detail ->
            when (detail) {
                SpaceDetail.SEATS -> seats = null
                SpaceDetail.BEDS -> beds = null
                SpaceDetail.ALTITUDE -> altitude = null
                SpaceDetail.COORDINATES -> {
                    latitude = null
                    longitude = null
                }
            }
        }
        try {
            validateSpaceDetails(seats, beds, latitude, longitude, altitude)
        } catch (e: IllegalArgumentException) {
            throw PatchRejectedException(Error.MalformedRequest())
        }
        val ownedFileIds = photos.map { it.id.value }
        request.photos?.forEach { photo ->
            val fileEntity = FileEntity.updateOrCreate(photo, ownedFileIds) { fileEntity ->
                SpaceImages.deleteWhere { (SpaceImages.space eq this@SpaceEntity.id) and (SpaceImages.file eq fileEntity.id) }
            }
            if (fileEntity != null) {
                SpaceImages.insert {
                    it[space] = this@SpaceEntity.id
                    it[file] = fileEntity.id
                }
            }
        }
        request.closedReason?.let { closedReason = it.takeUnlessEmpty() }
        request.closedUntil?.let { closedUntil = it }
        request.closedSince?.let { closedSince = it }
        request.isClosed?.let { closed ->
            isClosed = closed
            if (closed) {
                if (closedSince == null) closedSince = now()
            } else {
                closedSince = null
                closedUntil = null
                closedReason = null
            }
        }
    }

    override fun delete() {
        val photos = photos.toList()
        super.delete() // space_images cascades
        FileEntity.deleteOwnedFiles(photos)
    }

    companion object : UuidEntityClass<SpaceEntity>(Spaces)
}
