package org.centrexcursionistalcoi.app.network

import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.server.response.data.SectionD

object SectionsBackend {
    suspend fun list() = Backend.get("/sections", ListSerializer(SectionD.serializer()))

    suspend fun create(section: SectionD) = Backend.post(
        path = "/sections",
        body = section,
        bodySerializer = SectionD.serializer()
    )
}
