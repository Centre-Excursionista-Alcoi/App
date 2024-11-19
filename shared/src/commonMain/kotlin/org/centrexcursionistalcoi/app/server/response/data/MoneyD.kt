package org.centrexcursionistalcoi.app.server.response.data

import kotlinx.serialization.Serializable

@Serializable
data class MoneyD(
    val currency: String,
    val amount: Double,
    val factory: String
) {
    companion object
}
