package org.centrexcursionistalcoi.app.network

import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.SpaceBookingD
import org.centrexcursionistalcoi.app.data.SpaceD
import org.centrexcursionistalcoi.app.server.request.DateRangeRequest

object SpacesBackend {
    suspend fun list() = Backend.get("/spaces", ListSerializer(SpaceD.serializer()))

    suspend fun get(id: Int) = Backend.get("/spaces/$id", SpaceD.serializer())

    suspend fun create(space: SpaceD) = Backend.post(
        path = "/spaces",
        body = space,
        bodySerializer = SpaceD.serializer()
    )

    suspend fun update(space: SpaceD) = Backend.patch(
        path = "/spaces",
        body = space,
        bodySerializer = SpaceD.serializer()
    )

    suspend fun availability(from: Long, to: Long) = Backend.get(
        "/spaces/availability?from=${from}&to=${to}",
        ListSerializer(SpaceD.serializer())
    )

    suspend fun book(spaceId: Int, from: Long, to: Long) = Backend.post(
        path = "/spaces/$spaceId/book",
        body = DateRangeRequest(from, to),
        bodySerializer = DateRangeRequest.serializer()
    )

    /**
     * Lists all the bookings made by the user.
     */
    suspend fun listBookings() = Backend.get("/spaces/bookings", ListSerializer(SpaceBookingD.serializer()))
}
