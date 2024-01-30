package backend.database

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class InventoryItem(
    val id: Long,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("display_name") val displayName: String,
    @SerialName("display_name_localized") val localizedDisplayName: String?,
    val category: Long?
)
