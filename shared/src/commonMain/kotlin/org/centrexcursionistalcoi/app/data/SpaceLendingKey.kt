package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.InstantSerializer
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * How many keys of a [SpaceKey] a lending takes, and their hand-over and return.
 */
@Serializable
data class SpaceLendingKey(
    val key: Uuid,
    val quantity: Int,
    val givenBy: String? = null,
    @Serializable(InstantSerializer::class) val givenAt: Instant? = null,
    val returnedTo: String? = null,
    @Serializable(InstantSerializer::class) val returnedAt: Instant? = null,
)
