package backend.data.database

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class InventoryItem(
    val id: Long,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("display_name") val displayName: String,
    @SerialName("display_name_localized") val localizedDisplayName: String? = null,
    @SerialName("category") val categoryId: Long? = null,
    val description: String? = null
) {
    @Transient
    var category: Category? = null
}
