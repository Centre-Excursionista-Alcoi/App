package org.centrexcursionistalcoi.app.endpoints.space

import io.ktor.http.HttpMethod
import io.ktor.server.request.receive
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.Space
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.server.response.Errors
import org.centrexcursionistalcoi.app.server.response.data.SpaceD
import org.centrexcursionistalcoi.app.utils.toMonetaryAmount

object SpaceUpdateEndpoint : SecureEndpoint("/spaces", HttpMethod.Patch) {
    override suspend fun RoutingContext.secureBody(user: User) {
        if (!user.isAdmin) {
            respondFailure(Errors.Forbidden)
            return
        }

        val body = call.receive<SpaceD>()
        val id = body.id
        if (id == null) {
            respondFailure(Errors.MissingId)
            return
        }
        val result = ServerDatabase {
            Space.findById(id)
                ?.apply {
                    name = body.name
                    description = body.description

                    capacity = body.capacity?.toUInt()

                    memberPrice = body.memberPrice?.toMonetaryAmount()
                    externalPrice = body.externalPrice?.toMonetaryAmount()

                    setLocation(body.location)
                    setAddress(body.address)
                }
        }
        if (result == null) {
            respondFailure(Errors.ObjectNotFound)
            return
        }
        respondSuccess()
    }
}
