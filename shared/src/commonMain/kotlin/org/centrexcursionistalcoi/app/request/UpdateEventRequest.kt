package org.centrexcursionistalcoi.app.request

import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.Event
import org.centrexcursionistalcoi.app.data.FileWithContext

@Serializable
data class UpdateEventRequest(
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val place: String? = null,
    val title: String? = null,
    val description: String? = null,
    val maxPeople: Int? = null,
    val requiresConfirmation: Boolean? = null,
    val department: Uuid? = null,
    val image: FileWithContext? = null,
): UpdateEntityRequest<Uuid, Event> {
    override fun isEmpty(): Boolean {
        return date == null &&
            time == null &&
            place.isNullOrEmpty() &&
            title.isNullOrEmpty() &&
            description.isNullOrEmpty() &&
            maxPeople == null &&
            requiresConfirmation == null &&
            department == null &&
            (image == null || image.isEmpty())
    }
}
