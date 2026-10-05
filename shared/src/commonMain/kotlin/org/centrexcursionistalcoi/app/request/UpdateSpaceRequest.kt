package org.centrexcursionistalcoi.app.request

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.CategoryPrice
import org.centrexcursionistalcoi.app.data.Space
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Nullable fields are left as they are when null. To clear [conditionsOfUse] or [closedReason] send an empty
 * string. Opening the space (`isClosed = false`) clears the closed fields.
 */
@Serializable
data class UpdateSpaceRequest(
    val name: String? = null,
    val description: String? = null,
    val conditionsOfUse: String? = null,

    val prices: List<CategoryPrice>? = null,
    val requiresKeys: Boolean? = null,

    val isClosed: Boolean? = null,
    val closedSince: Instant? = null,
    val closedUntil: Instant? = null,
    val closedReason: String? = null,
): UpdateEntityRequest<Uuid, Space> {
    override fun isEmpty(): Boolean = name == null &&
            description == null &&
            conditionsOfUse == null &&
            prices == null &&
            requiresKeys == null &&
            isClosed == null &&
            closedSince == null &&
            closedUntil == null &&
            closedReason == null
}
