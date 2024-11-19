package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.enumeration.ItemHealth

@Serializable
data class ItemD(
    override val id: Int? = null,
    val createdAt: Long? = null,
    val health: ItemHealth = ItemHealth.NEW,
    val notes: String? = null,
    val typeId: Int? = null
): DatabaseData, Validator {
    override fun validate(): Boolean {
        return typeId != null
    }
}
