package org.centrexcursionistalcoi.app.data

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import org.centrexcursionistalcoi.app.serializer.InstantSerializer

/**
 * One thing that happened with the payments gateway, as the server recorded it. Only admins get them.
 */
@Serializable
data class PaymentAuditEntry(
    val id: Uuid,
    @Serializable(InstantSerializer::class) val createdAt: Instant,
    /** The transaction it is about, `null` if there is none (a report of an unknown payment...). */
    val transactionId: Uuid?,
    /** The provider's reference, to trace what it reported. */
    val referenceKey: String?,
    val provider: String?,
    val event: PaymentAuditEvent,
    /** `user:<sub>`, `provider` or `system`. */
    val actor: String,
    val previousStatus: PaymentStatus?,
    val newStatus: PaymentStatus?,
    /** In euro cents. */
    val amountCents: Int?,
    /** Anything else about the event: what the provider reported (sanitized), the error... */
    val detail: JsonObject?,
)
