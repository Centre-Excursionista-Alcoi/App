package org.centrexcursionistalcoi.app.endpoints.sections

import io.ktor.http.HttpMethod
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.data.SectionD
import org.centrexcursionistalcoi.app.database.entity.Section
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.endpoints.shared_logic.list.listAllDatabaseEntries

object ListSectionsEndpoint: SecureEndpoint("/sections", HttpMethod.Get) {
    override suspend fun RoutingContext.secureBody(user: User) {
        listAllDatabaseEntries(user, Section, SectionD.serializer())
    }
}
