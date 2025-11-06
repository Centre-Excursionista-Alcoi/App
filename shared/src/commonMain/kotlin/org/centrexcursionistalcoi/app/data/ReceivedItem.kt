package org.centrexcursionistalcoi.app.data

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
data class ReceivedItem(
    val id: Uuid,
    val lendingId: Uuid,
    val itemId: Uuid,
    val notes: String?,
    val receivedBy: String,
    val receivedAt: Instant,
)
