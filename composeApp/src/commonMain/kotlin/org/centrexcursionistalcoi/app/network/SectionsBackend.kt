package org.centrexcursionistalcoi.app.network

import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.SectionD

object SectionsBackend {
    suspend fun list() = Backend.get("/sections", ListSerializer(SectionD.serializer()))

    suspend fun create(section: SectionD) = Backend.post(
        path = "/sections",
        body = section,
        bodySerializer = SectionD.serializer()
    )

    suspend fun update(section: SectionD) = Backend.patch(
        path = "/sections",
        body = section,
        bodySerializer = SectionD.serializer()
    )
}
