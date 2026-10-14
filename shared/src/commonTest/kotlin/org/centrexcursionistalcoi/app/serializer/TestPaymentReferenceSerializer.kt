package org.centrexcursionistalcoi.app.serializer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.centrexcursionistalcoi.app.data.PaymentReference
import org.centrexcursionistalcoi.app.data.RedsysPayload
import org.centrexcursionistalcoi.app.data.RedsysReference
import org.centrexcursionistalcoi.app.json

class TestPaymentReferenceSerializer {
    @Test
    fun `redsys reference round trips without a type tag`() {
        val text = json.encodeToString<PaymentReference>(RedsysReference("1234ABC"))
        assertFalse("\"type\"" in text, text)
        val decoded = json.decodeFromString<PaymentReference>(text)
        assertIs<RedsysReference>(decoded)
        assertEquals("1234ABC", decoded.order)
        assertEquals("redsys", decoded.provider)
        assertEquals("redsys:1234ABC", decoded.key)
    }

    @Test
    fun `unknown references are rejected`() {
        assertFailsWith<SerializationException> { json.decodeFromString<PaymentReference>("""{"stripe_id":"pi_1"}""") }
    }

    private fun payload(cardNumber: String?) = RedsysPayload(
        order = "1234", response = "0000", amount = "1000", currency = "978",
        merchantCode = "999008881", terminal = "1", transactionType = "0", cardNumber = cardNumber,
    )

    @Test
    fun `sanitized keeps only the last four digits of the card`() {
        assertEquals("****0004", payload("454881******0004").sanitized().cardNumber)
        assertEquals("****1234", payload("4548 8100 0000 1234").sanitized().cardNumber)
        assertNull(payload("12").sanitized().cardNumber)
        assertNull(payload(null).sanitized().cardNumber)
    }

    @Test
    fun `sanitized changes nothing else`() {
        val original = payload("4548810000001234")
        assertEquals(original.copy(cardNumber = "****1234"), original.sanitized())
    }
}
