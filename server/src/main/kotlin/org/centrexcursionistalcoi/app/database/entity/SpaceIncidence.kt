package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.database.table.SpaceIncidencesTable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class SpaceIncidence(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<SpaceIncidence>(SpaceIncidencesTable)

    val createdAt by SpaceIncidencesTable.createdAt

    var title by SpaceIncidencesTable.title
    var body by SpaceIncidencesTable.body

    var booking by SpaceBooking referencedOn SpaceIncidencesTable.booking
}
