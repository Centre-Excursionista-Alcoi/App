package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * A type of key a [Space] needs, e.g. the main door or the car access. A lending can take up to [maxQuantity] of them.
 */
@Serializable
data class SpaceKey(
    override val id: Uuid,
    val lastUpdate: Instant,
    val space: Uuid,
    val name: String,
    val maxQuantity: Int,
) : Entity<Uuid>
