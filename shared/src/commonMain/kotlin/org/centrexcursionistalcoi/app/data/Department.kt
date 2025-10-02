package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

@Serializable
data class Department(
    val id: Long,
    val displayName: String,
    val imageFile: String?
)
