package org.centrexcursionistalcoi.app.database.entity

import java.time.ZoneOffset
import org.centrexcursionistalcoi.app.data.SpaceBookingD
import org.centrexcursionistalcoi.app.database.common.BookingEntity
import org.centrexcursionistalcoi.app.database.table.SpaceBookingsTable
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class SpaceBooking(id: EntityID<Int>) : BookingEntity(id) {
    companion object : IntEntityClass<SpaceBooking>(SpaceBookingsTable)

    val createdAt by SpaceBookingsTable.createdAt

    override var from by SpaceBookingsTable.from
    override var to by SpaceBookingsTable.to

    override var confirmed by SpaceBookingsTable.confirmed

    var key by SpaceKey optionalReferencedOn SpaceBookingsTable.key
    override var takenAt by SpaceBookingsTable.takenAt
    override var returnedAt by SpaceBookingsTable.returnedAt

    var paid by SpaceBookingsTable.paid
    var paymentReference by SpaceBookingsTable.paymentReference
    var paymentDocument by SpaceBookingsTable.paymentDocument

    var space by Space referencedOn SpaceBookingsTable.space
    override var user by User referencedOn SpaceBookingsTable.user

    fun serializable(): SpaceBookingD = SpaceBookingD(
        id = id.value,
        createdAt = createdAt.toEpochMilli(),
        from = from.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        to = to.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        userId = user.id.value,
        spaceId = space.id.value,
        confirmed = confirmed,
        keyId = key?.id?.value,
        takenAt = takenAt?.toEpochMilli(),
        returnedAt = returnedAt?.toEpochMilli(),
        paid = paid,
        paymentReference = paymentReference,
        paymentDocument = paymentDocument
    )
}
