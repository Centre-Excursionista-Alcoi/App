package org.centrexcursionistalcoi.app.request

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.data.ZonedDateTime

/**
 * `POST /memories` request body.
 */
@Serializable
data class CreateMemoryRequest(
    val text: String,
    val place: String? = null,
    /**
     * The member numbers of the members that took part.
     */
    val members: List<UInt> = emptyList(),
    val externalUsers: String? = null,
    val sport: Sports? = null,
    val department: Uuid? = null,
    /**
     * The lending the memory is for. The memory then takes its dates, and [from] and [to] are ignored.
     */
    val lending: Uuid? = null,
    /**
     * Required unless the memory is for a [lending].
     */
    val from: ZonedDateTime? = null,
    /**
     * Required unless the memory is for a [lending].
     */
    val to: ZonedDateTime? = null,
    val attachments: List<FileWithContext> = emptyList(),
) : RequestWithFiles<CreateMemoryRequest> {
    override fun mapFiles(transform: (FileWithContext) -> FileWithContext): CreateMemoryRequest =
        copy(attachments = attachments.map(transform))
}
