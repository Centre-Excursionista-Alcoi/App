package org.centrexcursionistalcoi.app.network

import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.SectionD
import org.centrexcursionistalcoi.app.database.entity.Section

object SectionsBackend {
    suspend fun list() = Backend.get("/sections", ListSerializer(SectionD.serializer()))
        .map(Section::deserialize)

    suspend fun create(section: Section) = Backend.post(
        path = "/sections",
        body = section.serializable(),
        bodySerializer = SectionD.serializer()
    )

    suspend fun update(section: Section) = Backend.patch(
        path = "/sections",
        body = section.serializable(),
        bodySerializer = SectionD.serializer()
    )
}
