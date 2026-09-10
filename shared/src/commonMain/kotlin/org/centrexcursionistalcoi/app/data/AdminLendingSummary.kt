package org.centrexcursionistalcoi.app.data

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.InstantSerializer

/**
 * Summary of a single [Lending] for the admin user detail page. Deliberately doesn't include a "confirmed by"
 * actor -- [org.centrexcursionistalcoi.app.database.table.Lendings]' `confirmed` column is a plain boolean with
 * no audit trail. [givenByName] (who physically handed the material over at pickup) and the returned-items'
 * own `receivedBy` are the only genuinely audited actors in the lending lifecycle.
 */
@Serializable
data class AdminLendingSummary(
    val id: Uuid,
    @Serializable(InstantSerializer::class) val timestamp: Instant,
    val from: LocalDate,
    val to: LocalDate,
    val confirmed: Boolean,
    val taken: Boolean,
    val givenByName: String? = null,
    @Serializable(InstantSerializer::class) val givenAt: Instant? = null,
    val returned: Boolean,
    val memorySubmitted: Boolean,
    @Serializable(InstantSerializer::class) val memorySubmittedAt: Instant? = null,
    val memoryReviewed: Boolean,
    val memoryId: Uuid? = null,
    val notes: String? = null,
    val items: List<String>,
)
