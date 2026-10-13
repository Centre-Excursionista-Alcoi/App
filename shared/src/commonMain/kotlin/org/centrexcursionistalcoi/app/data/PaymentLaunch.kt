package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Where to send the user to pay: [url] is a page of the server to open in a browser, which takes them to the payment
 * provider. It stops working after [expiresIn] seconds.
 */
@Serializable
data class PaymentLaunch(
    val url: String,
    @SerialName("expires_in") val expiresIn: Long,
)
