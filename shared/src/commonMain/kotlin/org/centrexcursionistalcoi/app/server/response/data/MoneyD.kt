package org.centrexcursionistalcoi.app.server.response.data

import kotlinx.serialization.Serializable

@Serializable
data class MoneyD(
    val currency: String = "EUR",
    val amount: Double
) {
    companion object
}
