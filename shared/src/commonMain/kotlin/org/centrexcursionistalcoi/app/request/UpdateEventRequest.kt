package org.centrexcursionistalcoi.app.request

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.Event
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.serializer.InstantSerializer

@Serializable
data class UpdateEventRequest(
    @Serializable(InstantSerializer::class) val start: Instant? = null,
    @Serializable(InstantSerializer::class) val end: Instant? = null,
    val place: String? = null,
    val title: String? = null,
    val description: String? = null,
    val maxPeople: Long? = null,
    val requiresConfirmation: Boolean? = null,
    val department: Uuid? = null,
    val image: FileWithContext? = null,
): UpdateEntityRequest<Uuid, Event> {
    override fun isEmpty(): Boolean {
        return start == null &&
            end == null &&
            place.isNullOrEmpty() &&
            title.isNullOrEmpty() &&
            description.isNullOrEmpty() &&
            maxPeople == null &&
            requiresConfirmation == null &&
            department == null &&
            (image == null || image.isEmpty())
    }
}
