package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.database.table.SpaceBookingsTable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class SpaceBooking(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<SpaceBooking>(SpaceBookingsTable)

    val createdAt by SpaceBookingsTable.createdAt

    var from by SpaceBookingsTable.from
    var to by SpaceBookingsTable.to

    var confirmed by SpaceBookingsTable.confirmed

    var key by SpaceKey optionalReferencedOn SpaceBookingsTable.key
    var takenAt by SpaceBookingsTable.takenAt

    var paid by SpaceBookingsTable.paid
    var paymentReference by SpaceBookingsTable.paymentReference
    var paymentDocument by SpaceBookingsTable.paymentDocument

    var space by Space referencedOn SpaceBookingsTable.space
    var user by User referencedOn SpaceBookingsTable.user
}
