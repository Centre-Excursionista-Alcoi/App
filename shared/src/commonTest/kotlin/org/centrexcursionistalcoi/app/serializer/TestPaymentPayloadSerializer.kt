package org.centrexcursionistalcoi.app.serializer

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import org.centrexcursionistalcoi.app.data.PaymentPayload
import org.centrexcursionistalcoi.app.data.RedsysPayload
import org.centrexcursionistalcoi.app.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs

class TestPaymentPayloadSerializer {
    private val redsys = RedsysPayload(
        order = "1234", response = "0000", amount = "1000", currency = "978",
        merchantCode = "999008881", terminal = "1", transactionType = "0",
    )

    @Test
    fun `redsys round trips without a type tag`() {
        val text = json.encodeToString<PaymentPayload>(redsys)
        assertFalse("\"type\"" in text, text)
        val decoded = json.decodeFromString<PaymentPayload>(text)
        assertIs<RedsysPayload>(decoded)
        assertEquals(redsys, decoded)
        assertEquals("1234", decoded.orderId)
    }

    @Test
    fun `unknown payloads are rejected`() {
        assertFailsWith<SerializationException> { json.decodeFromString<PaymentPayload>("""{"object":"charge"}""") }
    }
}
