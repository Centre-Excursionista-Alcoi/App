package backend.data.database

import backend.int.IconProvider
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: Long,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("display_name") val displayName: String,
    override val icon: String?
): IconProvider
