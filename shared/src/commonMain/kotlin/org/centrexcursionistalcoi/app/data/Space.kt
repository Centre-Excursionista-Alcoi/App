package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class Space(
    override val id: Uuid,
    val lastUpdate: Instant,

    val name: String,
    val description: String,
    val conditionsOfUse: String?,

    val requiresKeys: Boolean,

    val prices: List<CategoryPrice>,

    val isClosed: Boolean,
    val closedSince: Instant?,
    val closedUntil: Instant?,
    val closedReason: String?,
): Entity<Uuid>
