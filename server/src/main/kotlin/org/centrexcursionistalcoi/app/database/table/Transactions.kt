package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.data.PaymentStatus
import org.centrexcursionistalcoi.app.data.PaymentPayload
import org.centrexcursionistalcoi.app.serializer.PaymentPayloadSerializer
import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.centrexcursionistalcoi.app.json
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb

object Transactions : UuidTable("transactions") {
    val createdAt = timestamp("createdAt").defaultExpression(DatabaseNowExpression)
    val updatedAt = timestamp("updatedAt").defaultExpression(DatabaseNowExpression)

    val orderId = text("orderId")
    val amount = double("amount")
    val status = enumerationByName<PaymentStatus>("status", 20)
    /** What the payment provider reported, in its own format (see [PaymentPayload]). */
    val payload = jsonb("payload", json, PaymentPayloadSerializer).nullable()
}
