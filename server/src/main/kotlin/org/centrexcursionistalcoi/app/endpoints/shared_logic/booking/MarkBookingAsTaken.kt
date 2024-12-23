package org.centrexcursionistalcoi.app.endpoints.shared_logic.booking

import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.RoutingContext
import java.time.Instant
import org.centrexcursionistalcoi.app.data.IBookingD
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.common.BookingEntity
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.space.SpaceBookingMarkTakenEndpoint.respondFailure
import org.centrexcursionistalcoi.app.endpoints.space.SpaceBookingMarkTakenEndpoint.respondSuccess
import org.centrexcursionistalcoi.app.server.response.Errors
import org.jetbrains.exposed.dao.IntEntityClass

suspend fun <Serializable : IBookingD, Entity : BookingEntity<Serializable>, EntityClass : IntEntityClass<Entity>> RoutingContext.markBookingAsTaken(
    user: User,
    entityClass: EntityClass,
    extraBookingValidations: suspend RoutingContext.(Entity) -> Boolean = { true },
    extraDatabaseUpdates: (Entity) -> Unit = {}
) {
    // Verify that the user is admin
    if (!user.isAdmin) {
        respondFailure(Errors.Forbidden)
        return
    }

    // Verify that the booking is valid
    val booking = call.parameters["id"]
        ?.toIntOrNull()
        ?.let {
            ServerDatabase("MarkBookingAsTaken", "find${entityClass::class.simpleName}ById") { entityClass.findById(it) }
        }
    if (booking == null) {
        respondFailure(Errors.ObjectNotFound)
        return
    }

    // Verify that the booking is confirmed
    if (!booking.confirmed) {
        respondFailure(Errors.InvalidRequest)
        return
    }

    // Verify that the booking is not already taken
    if (booking.takenAt != null) {
        respondFailure(Errors.InvalidRequest)
        return
    }

    // Any extra validations to take into account
    if (!extraBookingValidations(booking)) {
        return
    }

    // Mark the booking as taken
    ServerDatabase("MarkBookingAsTaken", "updateTakenAt") {
        booking.takenAt = Instant.now()
        extraDatabaseUpdates(booking)
    }

    respondSuccess(HttpStatusCode.Accepted)
}
