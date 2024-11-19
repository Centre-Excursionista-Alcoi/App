package org.centrexcursionistalcoi.app.network

import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.SpaceD

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
}
