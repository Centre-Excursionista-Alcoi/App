package org.centrexcursionistalcoi.app.request

import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * `POST /inventory/lendings` request body.
 */
@Serializable
data class CreateLendingRequest(
    val from: LocalDate,
    val to: LocalDate,
    /**
     * The ids of the inventory items to lend.
     */
    val items: List<Uuid>,
    val notes: String? = null,
)

/**
 * `POST /inventory/lendings/{id}/pickup` request body. Optional: without it, no item is dismissed.
 */
@Serializable
data class PickupLendingRequest(
    /**
     * The ids of the items of the lending that are not given, and are removed from it.
     */
    val dismissItems: List<Uuid> = emptyList(),
)
