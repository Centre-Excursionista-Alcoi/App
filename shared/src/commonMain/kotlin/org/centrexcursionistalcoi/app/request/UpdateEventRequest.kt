package org.centrexcursionistalcoi.app.request

import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.Event
import org.centrexcursionistalcoi.app.data.FileWithContext

@Serializable
data class UpdateEventRequest(
    val start: LocalDateTime? = null,
    val end: LocalDateTime? = null,
    val place: String? = null,
    val title: String? = null,
    val description: String? = null,
    val maxPeople: Int? = null,
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
