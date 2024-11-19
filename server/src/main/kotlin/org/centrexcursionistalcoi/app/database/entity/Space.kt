package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.data.Serializable
import org.centrexcursionistalcoi.app.database.table.SpacesTable
import org.centrexcursionistalcoi.app.server.response.data.Address
import org.centrexcursionistalcoi.app.server.response.data.Location
import org.centrexcursionistalcoi.app.server.response.data.SpaceD
import org.centrexcursionistalcoi.app.utils.serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Space(id: EntityID<Int>) : IntEntity(id), Serializable<SpaceD> {
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
        }
    )
}
