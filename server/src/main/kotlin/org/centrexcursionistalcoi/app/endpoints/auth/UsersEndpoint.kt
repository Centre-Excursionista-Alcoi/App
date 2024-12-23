package org.centrexcursionistalcoi.app.endpoints.auth

import io.ktor.http.HttpMethod
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.UserD
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.server.response.Errors

object UsersEndpoint: SecureEndpoint("/users", HttpMethod.Get) {
    override suspend fun RoutingContext.secureBody(user: User) {
        if (!user.isAdmin) {
            respondFailure(Errors.Forbidden)
            return
        }

        val users = ServerDatabase("UsersEndpoint", "getUsers") { User.all().map(User::serializable) }
        respondSuccess(users, ListSerializer(UserD.serializer()))
    }
}
