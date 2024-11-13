package org.centrexcursionistalcoi.app.endpoints.inventory

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.RoutingContext
import kotlin.io.encoding.ExperimentalEncodingApi
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.ItemType
import org.centrexcursionistalcoi.app.database.entity.Section
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.server.response.Errors
import org.centrexcursionistalcoi.app.server.response.data.ItemTypeD

object CreateTypesEndpoint : SecureEndpoint("/inventory/types", HttpMethod.Post) {
    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun RoutingContext.secureBody(user: User) {
        if (!user.isAdmin) {
            respondFailure(Errors.Forbidden)
            return
        }

        val body = call.receive<ItemTypeD>()

        // Make sure that the section exists
        val sectionId = body.sectionId
        if (sectionId == null) {
            respondFailure(Errors.InvalidRequest)
            return
        }
        val itemSection = ServerDatabase { Section.findById(sectionId) }
        if (itemSection == null) {
            respondFailure(Errors.InvalidRequest)
            return
        }

        ServerDatabase {
            ItemType.new {
                title = body.title
                description = body.description
                brand = body.brand
                model = body.model
                image = body.imageBytes()
                section = itemSection
            }
        }
        respondSuccess(HttpStatusCode.Created)
    }
}
