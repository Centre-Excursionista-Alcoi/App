package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

@Serializable
data class Department(
    val id: Int,
    val displayName: String,
    val imageFile: String?
)
