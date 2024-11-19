package org.centrexcursionistalcoi.app.endpoints.sections

import io.ktor.http.HttpMethod
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.SectionD
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.Section
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint

object ListSectionsEndpoint: SecureEndpoint("/sections", HttpMethod.Get) {
    override suspend fun RoutingContext.secureBody(user: User) {
        val items = ServerDatabase {
            Section.all().map(Section::serializable)
        }
        respondSuccess(
            items, ListSerializer(SectionD.serializer())
        )
    }
}
