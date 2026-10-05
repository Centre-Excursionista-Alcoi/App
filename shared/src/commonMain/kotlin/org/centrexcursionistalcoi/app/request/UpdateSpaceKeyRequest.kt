package org.centrexcursionistalcoi.app.request

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.SpaceKey
import kotlin.uuid.Uuid

@Serializable
data class UpdateSpaceKeyRequest(
    val name: String? = null,
    val maxQuantity: Int? = null,
) : UpdateEntityRequest<Uuid, SpaceKey> {
    override fun isEmpty(): Boolean = name == null && maxQuantity == null
}
