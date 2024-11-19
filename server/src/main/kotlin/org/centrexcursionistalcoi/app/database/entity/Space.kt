package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.database.table.SpacesTable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Space(id: EntityID<Int>) : IntEntity(id) {
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
}
