package org.centrexcursionistalcoi.app.request

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.serializer.InstantSerializer

/**
 * `POST /events` request body, JSON-only (#659) -- the same shape [UpdateEventRequest] already uses for a patch,
 * with `start`/`place`/`title` required since an event can't exist without them.
 */
@Serializable
data class CreateEventRequest(
    @Serializable(InstantSerializer::class) val start: Instant,
    val place: String,
    val title: String,
    @Serializable(InstantSerializer::class) val end: Instant? = null,
    val description: String? = null,
    val maxPeople: Long? = null,
    val requiresConfirmation: Boolean = false,
    val requiresInsurance: Boolean = false,
    val department: Uuid? = null,
    val image: FileWithContext? = null,
    /** See [org.centrexcursionistalcoi.app.data.Event.qualificationRequirements]. */
    val qualificationRequirements: List<List<Uuid>> = emptyList(),
)
