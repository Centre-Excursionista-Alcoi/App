package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

@Serializable
data class MoneyD(
    val currency: String = "EUR",
    val amount: Double
)
