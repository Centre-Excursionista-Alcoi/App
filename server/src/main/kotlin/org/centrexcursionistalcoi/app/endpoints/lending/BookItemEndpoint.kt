package org.centrexcursionistalcoi.app.endpoints.lending

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.RoutingContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.Item
import org.centrexcursionistalcoi.app.database.entity.Lending
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.database.table.LendingsTable
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.server.request.LendingRequest
import org.centrexcursionistalcoi.app.server.response.Errors
import org.jetbrains.exposed.sql.and

object BookItemEndpoint : SecureEndpoint("/lending", HttpMethod.Post) {
    override suspend fun RoutingContext.secureBody(user: User) {
        if (!user.confirmed) {
            respondFailure(Errors.UserNotConfirmed)
            return
        }

        val body = call.receive<LendingRequest>()

        // Verify that the item exists
        val item = ServerDatabase { Item.findById(body.itemId) }
        if (item == null) {
            respondFailure(Errors.ObjectNotFound)
            return
        }

        // Verify that the item is available
        val from = Instant.ofEpochMilli(body.from)
            .let { LocalDateTime.ofInstant(it, ZoneId.systemDefault()) }
        val to = Instant.ofEpochMilli(body.to)
            .let { LocalDateTime.ofInstant(it, ZoneId.systemDefault()) }
        val lendings = ServerDatabase {
            Lending.find {
                (LendingsTable.item eq item.id) and (LendingsTable.from less from) and (LendingsTable.to greater to)
            }.toList()
        }
        if (lendings.isNotEmpty()) {
            respondFailure(Errors.ObjectNotAvailable)
            return
        }

        // Create the lending
        ServerDatabase {
            Lending.new {
                this.item = item
                this.user = user
                this.from = from
                this.to = to
            }
        }

        respondSuccess(HttpStatusCode.Created)
    }
}
