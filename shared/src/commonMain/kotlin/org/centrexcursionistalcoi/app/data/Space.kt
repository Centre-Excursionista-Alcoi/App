package org.centrexcursionistalcoi.app.data

import kotlin.time.Duration
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
data class Space(
    override val id: Uuid,
    val name: String,
    val description: String?,
    /**
     * Pair of price (€) and duration (e.g., hourly, daily).
     */
    val price: Pair<Double, Duration>?,
    val capacity: Int?,
): Entity<Uuid> {
    override fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "description" to description,
        "price" to price?.first,
        "priceDuration" to price?.second,
        "capacity" to capacity,
    )
}
