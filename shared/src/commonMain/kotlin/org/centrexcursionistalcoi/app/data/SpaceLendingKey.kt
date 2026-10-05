package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.InstantSerializer
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * A [SpaceKey] a lending has taken: when it was handed out and by whom, and, once it is back, when and to whom.
 */
@Serializable
data class SpaceLendingKey(
    val key: Uuid,
    val givenBy: String? = null,
    @Serializable(InstantSerializer::class) val givenAt: Instant? = null,
    val returnedTo: String? = null,
    @Serializable(InstantSerializer::class) val returnedAt: Instant? = null,
) {
    /** Whether the key is still with the lending. */
    val isOut: Boolean get() = givenAt != null && returnedAt == null
}
