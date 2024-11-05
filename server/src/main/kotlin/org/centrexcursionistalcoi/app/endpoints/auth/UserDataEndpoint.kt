package org.centrexcursionistalcoi.app.endpoints.auth

import io.ktor.http.HttpMethod
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.server.response.data.UserD

object UserDataEndpoint: SecureEndpoint("/me", HttpMethod.Get) {
    override suspend fun RoutingContext.secureBody(user: User) {
        val response = ServerDatabase { user.serializable() }
        respondSuccess(response, UserD.serializer())
    }
}
