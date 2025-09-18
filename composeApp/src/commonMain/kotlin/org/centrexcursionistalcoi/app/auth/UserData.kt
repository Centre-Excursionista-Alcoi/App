package org.centrexcursionistalcoi.app.auth

import kotlinx.serialization.Serializable

@Serializable
data class UserData(
    val name: String,
    val groups: List<String>
)
