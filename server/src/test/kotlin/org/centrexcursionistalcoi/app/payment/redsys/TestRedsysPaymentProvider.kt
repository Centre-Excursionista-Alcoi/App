package org.centrexcursionistalcoi.app.payment.redsys

import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.centrexcursionistalcoi.app.data.PaymentMethod
import org.centrexcursionistalcoi.app.data.PaymentStatus
import org.centrexcursionistalcoi.app.data.RedsysPayload
import org.centrexcursionistalcoi.app.data.RedsysReference
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.payment.InvalidPaymentNotificationException
import org.centrexcursionistalcoi.app.payment.PaymentRequest
import org.centrexcursionistalcoi.app.payment.PaymentSession
import org.centrexcursionistalcoi.app.payment.RawPaymentNotification
import org.centrexcursionistalcoi.app.payment.UnsupportedPaymentMethodException

class TestRedsysPaymentProvider {
    private val key = RedsysConfig.SANDBOX_KEY
    private fun provider(bizum: Boolean = false) = RedsysPaymentProvider(
        RedsysConfig("999008881", "1", key, bizum = bizum),
        notificationUrl = "https://app.example/payments/redsys/notification",
    )

    private fun request(method: PaymentMethod = PaymentMethod.ANY) = PaymentRequest(
        amountCents = 600, description = "Night in the refuge", successUrl = "https://app.example/ok",
        failureUrl = "https://app.example/ko", method = method, language = "ca-ES", payerPhone = "+34700000000",
    )

    private fun decode(form: PaymentSession.Form): JsonObject =
        json.parseToJsonElement(String(Base64.getUrlDecoder().decode(form.fields.getValue("Ds_MerchantParameters")))).jsonObject

    private suspend fun create(provider: RedsysPaymentProvider, method: PaymentMethod = PaymentMethod.ANY): PaymentSession.Form =
        assertIs(provider.createPayment(request(method)))

    /** What Redsys sends to the notification URL for [payload] (JSON), signed with [signKey]. */
    private fun notification(payload: String, order: String = "1234ABCD", signKey: String = key): RawPaymentNotification {
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        return RawPaymentNotification(
            parameters = mapOf(
                "Ds_SignatureVersion" to RedsysSignature.VERSION,
                "Ds_MerchantParameters" to encoded,
                "Ds_Signature" to RedsysSignature.sign(signKey, order, encoded),
            )
        )
    }

    private fun payload(response: String = "0000", type: String = "0", order: String = "1234ABCD", extra: String = "") =
        """{"Ds_Order":"$order","Ds_Response":"$response","Ds_Amount":"600","Ds_Currency":"978","Ds_MerchantCode":"999008881","Ds_Terminal":"1","Ds_TransactionType":"$type"$extra}"""

    @Test
    fun `the payment is a signed form to the hosted page`() = runTest {
        val form = create(provider())

        assertEquals("https://sis-t.redsys.es:25443/sis/realizarPago", form.url)
        assertEquals("HMAC_SHA512_V2", form.fields["Ds_SignatureVersion"])
        val parameters = decode(form)
        val order = parameters.getValue("DS_MERCHANT_ORDER").jsonPrimitive.content
        assertTrue(Regex("^[0-9]{4}[A-Z0-9]{8}$").matches(order), order)
        assertEquals(RedsysReference(order), form.reference)
        assertTrue(RedsysSignature.verify(key, order, form.fields.getValue("Ds_MerchantParameters"), form.fields.getValue("Ds_Signature")))
        assertEquals("600", parameters.getValue("DS_MERCHANT_AMOUNT").jsonPrimitive.content)
        assertEquals("978", parameters.getValue("DS_MERCHANT_CURRENCY").jsonPrimitive.content)
        assertEquals("0", parameters.getValue("DS_MERCHANT_TRANSACTIONTYPE").jsonPrimitive.content)
        assertEquals("999008881", parameters.getValue("DS_MERCHANT_MERCHANTCODE").jsonPrimitive.content)
        assertEquals("https://app.example/payments/redsys/notification", parameters.getValue("DS_MERCHANT_MERCHANTURL").jsonPrimitive.content)
        assertEquals("https://app.example/ok", parameters.getValue("DS_MERCHANT_URLOK").jsonPrimitive.content)
        assertEquals("https://app.example/ko", parameters.getValue("DS_MERCHANT_URLKO").jsonPrimitive.content)
        assertEquals("003", parameters.getValue("DS_MERCHANT_CONSUMERLANGUAGE").jsonPrimitive.content)
        assertNull(parameters["DS_MERCHANT_PAYMETHODS"])
    }

    @Test
    fun `every payment gets its own order`() = runTest {
        val provider = provider()
        assertTrue(List(20) { create(provider).reference.key }.toSet().size > 1)
    }

    @Test
    fun `bizum is only offered when enabled`() = runTest {
        assertEquals(setOf(PaymentMethod.CARD), provider().methods)
        assertEquals(setOf(PaymentMethod.CARD, PaymentMethod.BIZUM), provider(bizum = true).methods)
        assertFailsWith<UnsupportedPaymentMethodException> { create(provider(), PaymentMethod.BIZUM) }

        val parameters = decode(create(provider(bizum = true), PaymentMethod.BIZUM))
        assertEquals("z", parameters.getValue("DS_MERCHANT_PAYMETHODS").jsonPrimitive.content)
        assertEquals("+34700000000", parameters.getValue("DS_MERCHANT_BIZUM_MOBILENUMBER").jsonPrimitive.content)
        assertEquals("C", decode(create(provider(), PaymentMethod.CARD)).getValue("DS_MERCHANT_PAYMETHODS").jsonPrimitive.content)
    }

    @Test
    fun `a signed notification is read and the card is censored`() = runTest {
        val result = provider().handleNotification(notification(payload(extra = ""","Ds_Card_Number":"454881******0004","Ds_AuthorisationCode":"842841"""")))

        assertEquals(RedsysReference("1234ABCD"), result.reference)
        assertEquals(PaymentStatus.COMPLETED, result.status)
        assertEquals(600, result.amountCents)
        val stored = assertIs<RedsysPayload>(result.payload)
        assertEquals("****0004", stored.cardNumber)
        assertEquals("842841", stored.authorisationCode)
    }

    @Test
    fun `a notification that fails is reported as failed`() = runTest {
        assertEquals(PaymentStatus.FAILED, provider().handleNotification(notification(payload(response = "0190"))).status)
        assertEquals(PaymentStatus.FAILED, provider().handleNotification(notification(payload(response = "oops"))).status)
    }

    @Test
    fun `notifications are rejected unless properly signed`() = runTest {
        val provider = provider()
        suspend fun rejected(notification: RawPaymentNotification) =
            assertFailsWith<InvalidPaymentNotificationException> { provider.handleNotification(notification) }

        rejected(notification(payload(), signKey = "another key of 16+ chars"))
        rejected(notification(payload(), order = "OTHER123"))
        val good = notification(payload())
        rejected(good.copy(parameters = good.parameters + ("Ds_SignatureVersion" to "HMAC_SHA256_V1")))
        rejected(good.copy(parameters = good.parameters - "Ds_Signature"))
        rejected(good.copy(parameters = good.parameters + ("Ds_MerchantParameters" to "not base64!")))
        rejected(notification("[]"))
        rejected(notification("""{"Ds_Order":"1234ABCD"}"""))
        rejected(RawPaymentNotification())
    }

    @Test
    fun `the meaning of a response depends on the operation`() {
        fun status(response: String, type: String) = RedsysPaymentProvider.statusOf(
            RedsysPayload("1234", response, "100", "978", "1", "1", type)
        )

        assertEquals(PaymentStatus.COMPLETED, status("0000", "0"))
        assertEquals(PaymentStatus.COMPLETED, status("0099", "1"))
        assertEquals(PaymentStatus.FAILED, status("0100", "0"))
        assertEquals(PaymentStatus.REFUNDED, status("0900", "3"))
        assertEquals(PaymentStatus.REFUNDED, status("0900", "45"))
        assertEquals(PaymentStatus.FAILED, status("0190", "3"))
        assertEquals(PaymentStatus.CANCELLED, status("0400", "9"))
        assertEquals(PaymentStatus.FAILED, status("0900", "9"))
    }

}
