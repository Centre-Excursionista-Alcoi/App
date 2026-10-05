package org.centrexcursionistalcoi.app.request

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.CategoryPrice

@Serializable
data class CreateSpaceRequest(
    val name: String,
    val description: String,
    val conditionsOfUse: String? = null,
    val requiresKeys: Boolean = false,
    val prices: List<CategoryPrice> = emptyList(),
)
