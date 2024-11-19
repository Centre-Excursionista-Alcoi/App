package org.centrexcursionistalcoi.app.database.entity

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import org.centrexcursionistalcoi.app.data.Address
import org.centrexcursionistalcoi.app.data.Location
import org.centrexcursionistalcoi.app.data.SpaceD
import org.centrexcursionistalcoi.app.database.common.SerializableEntity
import org.centrexcursionistalcoi.app.database.table.SpaceKeysTable
import org.centrexcursionistalcoi.app.database.table.SpacesImagesTable
import org.centrexcursionistalcoi.app.database.table.SpacesTable
import org.centrexcursionistalcoi.app.utils.serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Space(id: EntityID<Int>) : IntEntity(id), SerializableEntity<SpaceD> {
    companion object : IntEntityClass<Space>(SpacesTable)

    val createdAt by SpacesTable.createdAt

    var name by SpacesTable.name
    var description by SpacesTable.description

    var capacity by SpacesTable.capacity

    var memberPrice by SpacesTable.memberPrice
    var externalPrice by SpacesTable.externalPrice

    var latitude by SpacesTable.latitude
    var longitude by SpacesTable.longitude

    var address by SpacesTable.address
    var city by SpacesTable.city
    var postalCode by SpacesTable.postalCode
    var country by SpacesTable.country

    fun setAddress(address: Address?) {
        address ?: return
        this.address = address.address
        this.city = address.city
        this.postalCode = address.postalCode
        this.country = address.country
    }

    fun setLocation(location: Location?) {
        location ?: return
        latitude = location.latitude
        longitude = location.longitude
    }

    @ExperimentalEncodingApi
    override fun serializable(): SpaceD = SpaceD(
        id = id.value,
        createdAt = createdAt.toEpochMilli(),
        name = name,
        description = description,
        capacity = capacity?.toInt(),
        memberPrice = memberPrice?.serializable(),
        externalPrice = externalPrice?.serializable(),
        location = (latitude to longitude).let { (lat, lon) ->
            if (lat != null && lon != null) Location(lat, lon) else null
        },
        address = if (address != null || city != null || postalCode != null || country != null) {
            Address(address, city, postalCode, country)
        } else {
            null
        },
        images = SpaceImage.find { SpacesImagesTable.space eq id }
            .map { it.image }
            .map { Base64.encode(it) },
        keys = SpaceKey.find { SpaceKeysTable.space eq id }
            .map(SpaceKey::serializable)
    )
}
