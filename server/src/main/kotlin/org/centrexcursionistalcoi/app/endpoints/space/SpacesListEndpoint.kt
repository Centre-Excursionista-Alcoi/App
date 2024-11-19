package org.centrexcursionistalcoi.app.endpoints.space

import io.ktor.http.HttpMethod
import io.ktor.server.routing.RoutingContext
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.SpaceD
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.Space
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.server.response.Errors

object SpacesListEndpoint: SecureEndpoint("/spaces", HttpMethod.Get) {
    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun RoutingContext.secureBody(user: User) {
        if (!user.confirmed) {
            respondFailure(Errors.UserNotConfirmed)
            return
        }

        val list = ServerDatabase {
            Space.all().map(Space::serializable)
        }
        respondSuccess(
            list, ListSerializer(SpaceD.serializer())
        )
    }
}
