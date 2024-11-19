package org.centrexcursionistalcoi.app.server.response.data

import kotlinx.serialization.Serializable

@Serializable
data class Address(
    val address: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
)
