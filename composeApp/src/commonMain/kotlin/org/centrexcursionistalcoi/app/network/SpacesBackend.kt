package org.centrexcursionistalcoi.app.network

import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.SpaceBookingD
import org.centrexcursionistalcoi.app.data.SpaceD
import org.centrexcursionistalcoi.app.database.entity.Space
import org.centrexcursionistalcoi.app.database.entity.SpaceBooking
import org.centrexcursionistalcoi.app.server.request.DateRangeRequest

object SpacesBackend {
    suspend fun list() = Backend.get("/spaces", ListSerializer(SpaceD.serializer()))
        .map(Space::deserialize)

    suspend fun get(id: Int) = Backend.get("/spaces/$id", SpaceD.serializer())
        .let(Space::deserialize)

    suspend fun create(space: Space) = Backend.post(
        path = "/spaces",
        body = space.serializable(),
        bodySerializer = SpaceD.serializer()
    )

    suspend fun update(space: Space) = Backend.patch(
        path = "/spaces",
        body = space.serializable(),
        bodySerializer = SpaceD.serializer()
    )

    suspend fun availability(from: LocalDate, to: LocalDate) = Backend.get(
        "/spaces/availability?from=${from}&to=${to}",
        ListSerializer(SpaceD.serializer())
    ).map(Space::deserialize)

    suspend fun book(spaceId: Int, from: LocalDate, to: LocalDate) = Backend.post(
        path = "/spaces/$spaceId/book",
        body = DateRangeRequest(from, to),
        bodySerializer = DateRangeRequest.serializer()
    )

    /**
     * Lists all the bookings made by the user.
     */
    suspend fun listBookings() = Backend.get("/spaces/bookings", ListSerializer(SpaceBookingD.serializer()))
        .map(SpaceBooking::deserialize)

    /**
     * Lists all the bookings made by all users.
     */
    suspend fun allBookings() = Backend.get("/spaces/bookings?all=true", ListSerializer(SpaceBookingD.serializer()))
        .map(SpaceBooking::deserialize)

    suspend fun getBooking(id: Int) = Backend.get(
        path = "/spaces/bookings/$id",
        deserializer = SpaceBookingD.serializer()
    ).let(SpaceBooking::deserialize)

    suspend fun confirm(bookingId: Int) = Backend.post(
        path = "/spaces/bookings/$bookingId/confirm"
    )

    suspend fun markTaken(bookingId: Int) = Backend.post(
        path = "/spaces/bookings/$bookingId/taken"
    )

    suspend fun markTaken(bookingId: Int, keyId: Int) = Backend.post(
        path = "/spaces/bookings/$bookingId/taken/$keyId"
    )

    suspend fun markReturned(bookingId: Int) = Backend.post(
        path = "/spaces/bookings/$bookingId/returned"
    )

    suspend fun cancelBooking(bookingId: Int) = Backend.delete(
        path = "/spaces/bookings/$bookingId"
    )
}
