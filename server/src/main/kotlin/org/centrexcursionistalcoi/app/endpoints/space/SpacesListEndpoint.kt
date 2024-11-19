package org.centrexcursionistalcoi.app.endpoints.space

import io.ktor.http.HttpMethod
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.data.SpaceD
import org.centrexcursionistalcoi.app.database.entity.Space
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.endpoints.shared_logic.list.listAllDatabaseEntries

object SpacesListEndpoint: SecureEndpoint("/spaces", HttpMethod.Get) {
    override suspend fun RoutingContext.secureBody(user: User) {
        listAllDatabaseEntries(user, Space, SpaceD.serializer())
    }
}
