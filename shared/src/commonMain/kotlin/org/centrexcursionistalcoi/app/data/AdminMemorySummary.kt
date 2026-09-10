package org.centrexcursionistalcoi.app.data

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.InstantSerializer

/**
 * Summary of a single [Memory] for the admin user detail page.
 */
@Serializable
data class AdminMemorySummary(
    val id: Uuid,
    val text: String,
    val place: String? = null,
    val externalUsers: String? = null,
    val sport: Sports? = null,
    val from: ZonedDateTime,
    val to: ZonedDateTime,
    @Serializable(InstantSerializer::class) val submittedAt: Instant,
    val lendingId: Uuid? = null,
    val pdfId: Uuid? = null,
    val attachmentIds: List<Uuid>,
)
