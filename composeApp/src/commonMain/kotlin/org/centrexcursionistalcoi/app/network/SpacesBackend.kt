package org.centrexcursionistalcoi.app.network

import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.server.response.data.SpaceD

object SpacesBackend {
    suspend fun list() = Backend.get("/spaces", ListSerializer(SpaceD.serializer()))

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
}
