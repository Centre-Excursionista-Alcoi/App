package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
data class LendingMemory(
    val place: String?,
    val memberUsers: List<Uuid>,
    val externalUsers: String?,
    val text: String,
    val files: List<Uuid>,
)
