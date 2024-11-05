package org.centrexcursionistalcoi.app.server.response.data

import kotlinx.serialization.Serializable

@Serializable
data class UserD(
    val email: String,
    val isAdmin: Boolean,
    val isConfirmed: Boolean,
    val name: String,
    val familyName: String,
    val nif: String,
    val phone: String
)
