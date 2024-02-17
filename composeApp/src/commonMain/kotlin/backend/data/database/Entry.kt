package backend.data.database

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Entry(
    val id: Int,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("inventory_item") val inventoryItemId: Long,
    @SerialName("made_by") val madeBy: String,
    @SerialName("confirmed_by") val confirmedBy: String? = null,
    @SerialName("collection_uuid") val collectionUuid: String,
    val returned: Boolean? = null
)
