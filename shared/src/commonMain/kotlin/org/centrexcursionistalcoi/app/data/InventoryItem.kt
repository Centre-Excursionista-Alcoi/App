package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
data class InventoryItem(
    override val id: Uuid,
    val variation: String?,
    val type: Uuid,
): Entity<Uuid> {
    override fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "variation" to variation,
        "type" to type,
    )
}
