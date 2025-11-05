package org.centrexcursionistalcoi.app.request

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
data class ReturnLendingRequest(
    val receivedBySub: String,
    /**
     * List of the items being returned.
     *
     * Can be all, or a subset of the items originally lent.
     */
    val returnedItems: List<ReturnedItem>,
) {
    @Serializable
    data class ReturnedItem(
        val itemId: Uuid,
        val notes: String? = null,
    )
}
