package org.centrexcursionistalcoi.app.request

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.Space

@Serializable
data class UpdateSpaceRequest(
    val name: String? = null,
    val description: String? = null,
    val price: Double? = null,
    val priceDurationSeconds: Long? = null,
    val capacity: Int? = null,
): UpdateEntityRequest<Uuid, Space> {
    override fun isEmpty(): Boolean {
        return name.isNullOrEmpty()
            && description.isNullOrEmpty()
            && price == null
            && priceDurationSeconds == null
            && capacity == null
    }
}
