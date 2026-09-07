package org.centrexcursionistalcoi.app.request

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.data.Memory
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import kotlin.uuid.Uuid

@Serializable
data class UpdateMemoryRequest(
    val place: String? = null,
    val members: List<UInt>? = null,
    val externalUsers: String? = null,
    val text: String? = null,
    val sport: Sports? = null,
    val department: Uuid? = null,
    val attachments: List<FileWithContext>? = null,
    val from: ZonedDateTime? = null,
    val to: ZonedDateTime? = null,
): UpdateEntityRequest<Uuid, Memory> {
    override fun isEmpty(): Boolean {
        return place.isNullOrEmpty() &&
            members.isNullOrEmpty() &&
            externalUsers.isNullOrEmpty() &&
            text.isNullOrEmpty() &&
            sport == null &&
            department == null &&
            attachments.isNullOrEmpty() &&
            from == null &&
            to == null
    }
}
