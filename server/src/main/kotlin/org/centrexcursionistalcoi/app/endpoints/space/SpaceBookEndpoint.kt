package org.centrexcursionistalcoi.app.endpoints.space

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.RoutingContext
import kotlinx.datetime.toJavaLocalDate
import org.centrexcursionistalcoi.app.data.SpaceD
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.Space
import org.centrexcursionistalcoi.app.database.entity.SpaceBooking
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.database.utils.spacesAvailableForDates
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.server.request.DateRangeRequest
import org.centrexcursionistalcoi.app.server.response.Errors

object SpaceBookEndpoint: SecureEndpoint("/spaces/{id}/book", HttpMethod.Post) {
    override suspend fun RoutingContext.secureBody(user: User) {
        if (!user.confirmed) {
            respondFailure(Errors.UserNotConfirmed)
            return
        }

        // Extract the request body
        val body = call.receive<DateRangeRequest>()

        // Extract the space id and check that it is a valid number
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            respondFailure(Errors.InvalidRequest)
            return
        }

        // Verify that the space exists
        val space = ServerDatabase { Space.findById(id) }
        if (space == null) {
            respondFailure(Errors.ObjectNotFound)
            return
        }

        // Verify that the space is available
        val from = body.from.toJavaLocalDate()
        val to = body.to.toJavaLocalDate()
        val availableSpaces = ServerDatabase { spacesAvailableForDates(from, to) }.map(SpaceD::id)
        if (id !in availableSpaces) {
            respondFailure(Errors.InvalidRequest)
            return
        }

        // Create the booking
        ServerDatabase {
            SpaceBooking.new {
                this.space = space
                this.user = user
                this.from = from
                this.to = to
            }
        }

        respondSuccess(HttpStatusCode.Created)
    }
}
