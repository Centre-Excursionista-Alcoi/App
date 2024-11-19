package org.centrexcursionistalcoi.app.endpoints.shared_logic.booking

import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.IBookingD
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.common.BookingEntity
import org.centrexcursionistalcoi.app.database.common.BookingTable
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.space.SpaceBookingsListEndpoint.respondFailure
import org.centrexcursionistalcoi.app.endpoints.space.SpaceBookingsListEndpoint.respondSuccess
import org.centrexcursionistalcoi.app.server.response.Errors
import org.jetbrains.exposed.dao.IntEntityClass

suspend fun <Serializable : IBookingD, Entity : BookingEntity<Serializable>, Table : BookingTable, EntityClass : IntEntityClass<Entity>> RoutingContext.listBookings(
    user: User,
    table: Table,
    entityClass: EntityClass,
    serializer: KSerializer<Serializable>
) {
    if (!user.confirmed) {
        respondFailure(Errors.UserNotConfirmed)
        return
    }

    val query = call.request.queryParameters
    val all = query["all"]?.toBoolean() ?: false

    val bookings = ServerDatabase {
        if (all && user.isAdmin) {
            entityClass.all()
        } else {
            entityClass.find { table.user eq user.id }
        }.map { it.serializable() }
    }
    respondSuccess(
        data = bookings,
        serializer = ListSerializer(serializer)
    )
}
