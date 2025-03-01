package org.centrexcursionistalcoi.app.endpoints.sections

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.data.SectionD
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.Section
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.server.response.Errors

object CreateSectionEndpoint : SecureEndpoint("/sections", HttpMethod.Post) {
    override suspend fun RoutingContext.secureBody(user: User) {
        if (!user.isAdmin) {
            respondFailure(Errors.Forbidden)
            return
        }

        val body = call.receive<SectionD>()
        ServerDatabase {
            Section.new {
                displayName = body.displayName
            }
        }
        respondSuccess(HttpStatusCode.Created)
    }
}
