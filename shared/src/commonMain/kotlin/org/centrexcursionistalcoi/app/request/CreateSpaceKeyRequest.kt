package org.centrexcursionistalcoi.app.request

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class CreateSpaceKeyRequest(
    val space: Uuid,
    val name: String,
    val maxQuantity: Int = 1,
)
