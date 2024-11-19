package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

@Serializable
data class Location(
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    fun orNull(): Location? = if (latitude == null && longitude == null) null else this
}
