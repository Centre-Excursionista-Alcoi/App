package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.InstantSerializer
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class Space(
    override val id: Uuid,
    val name: String,
    val description: String,
    val conditionsOfUse: String?,

    val requiresKeys: Boolean,

    val prices: List<CategoryPrice>,

    val isClosed: Boolean,
    @Serializable(InstantSerializer::class) val closedSince: Instant?,
    @Serializable(InstantSerializer::class) val closedUntil: Instant?,
    val closedReason: String?,
): Entity<Uuid>
