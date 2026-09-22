package org.centrexcursionistalcoi.app.request

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.FileWithContext

/**
 * `POST /inventory/types` request body, JSON-only (#659) -- the same shape [UpdateInventoryItemTypeRequest]
 * already uses for a patch, with `displayName` required since a type can't exist without one.
 */
@Serializable
data class CreateInventoryItemTypeRequest(
    val displayName: String,
    val description: String? = null,
    val categories: List<String>? = null,
    val weight: Double? = null,
    val department: Uuid? = null,
    val image: FileWithContext? = null,
)
