package org.centrexcursionistalcoi.app.endpoints.space

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.Space
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.server.response.Errors
import org.centrexcursionistalcoi.app.server.response.data.SpaceD
import org.centrexcursionistalcoi.app.utils.toMonetaryAmount

object SpaceCreateEndpoint : SecureEndpoint("/spaces", HttpMethod.Post) {
    override suspend fun RoutingContext.secureBody(user: User) {
        if (!user.isAdmin) {
            respondFailure(Errors.Forbidden)
            return
        }

        val body = call.receive<SpaceD>()
        ServerDatabase {
            Space.new {
                name = body.name
                description = body.description

                capacity = body.capacity?.toUInt()

                memberPrice = body.memberPrice?.toMonetaryAmount()
                externalPrice = body.externalPrice?.toMonetaryAmount()

                setLocation(body.location)
                setAddress(body.address)
            }
        }
        respondSuccess(HttpStatusCode.Created)
    }
}
