package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A Redsys payment, named by its order number (`^[a-zA-Z0-9]{4,12}$`, never reused). */
@Serializable
data class RedsysReference(
    @SerialName("redsys_order") val order: String,
) : PaymentReference {
    override val provider: String get() = PROVIDER

    override val key: String get() = "$PROVIDER:$order"

    companion object {
        const val PROVIDER = "redsys"
    }
}
