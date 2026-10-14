package org.centrexcursionistalcoi.app.payment.redsys

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.IOException
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.centrexcursionistalcoi.app.data.PaymentStatus
import org.centrexcursionistalcoi.app.data.RedsysReference
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.payment.PaymentGatewayUnavailableException
import org.centrexcursionistalcoi.app.payment.PaymentProviderException

/** Refunds and cancellations, server to server, against a fake Redsys. */
class TestRedsysRest {
    private val key = RedsysConfig.SANDBOX_KEY
    private val reference = RedsysReference("1234ABCD")

    /** A request as it reached Redsys, decoded. */
    private class Sent(val url: String, val contentType: String?, val type: String, val order: String, val amount: String, val signatureValid: Boolean)

    private suspend fun HttpRequestData.decode(): Sent {
        val body = json.parseToJsonElement(String(body.toByteArray())).jsonObject
        val encoded = body.getValue("Ds_MerchantParameters").jsonPrimitive.content
        val parameters = json.parseToJsonElement(String(Base64.getUrlDecoder().decode(encoded))).jsonObject
        val order = parameters.getValue("DS_MERCHANT_ORDER").jsonPrimitive.content
        return Sent(
            url = url.toString(),
            contentType = headers[HttpHeaders.ContentType] ?: this.body.contentType?.toString(),
            type = parameters.getValue("DS_MERCHANT_TRANSACTIONTYPE").jsonPrimitive.content,
            order = order,
            amount = parameters.getValue("DS_MERCHANT_AMOUNT").jsonPrimitive.content,
            signatureValid = RedsysSignature.verify(key, order, encoded, body.getValue("Ds_Signature").jsonPrimitive.content),
        )
    }

    private fun MockRequestHandleScope.signed(response: String, type: String, order: String = reference.order, signKey: String = key): HttpResponseData {
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(
            """{"Ds_Order":"$order","Ds_Response":"$response","Ds_Amount":"600","Ds_Currency":"978","Ds_MerchantCode":"999008881","Ds_Terminal":"1","Ds_TransactionType":"$type"}""".toByteArray()
        )
        return answer(
            """{"Ds_SignatureVersion":"HMAC_SHA512_V2","Ds_MerchantParameters":"$encoded","Ds_Signature":"${RedsysSignature.sign(signKey, order, encoded)}"}"""
        )
    }

    private fun MockRequestHandleScope.answer(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))

    private fun provider(requests: MutableList<Sent> = mutableListOf(), handler: suspend MockRequestHandleScope.(Sent) -> HttpResponseData) =
        RedsysPaymentProvider(
            RedsysConfig("999008881", "1", key),
            httpClient = HttpClient(MockEngine { request -> handler(request.decode().also(requests::add)) }),
        )

    @Test
    fun `a refund is a signed type 3 request to the REST endpoint`() = runTest {
        val sent = mutableListOf<Sent>()

        val result = provider(sent) { signed("0900", "3") }.refund(reference, 600)

        assertEquals(PaymentStatus.REFUNDED, result.status)
        assertEquals(reference, result.reference)
        val request = sent.single()
        assertEquals("https://sis-t.redsys.es:25443/sis/rest/trataPeticionREST", request.url)
        assertEquals("3", request.type)
        assertEquals("1234ABCD", request.order)
        assertEquals("600", request.amount)
        assertTrue(request.signatureValid)
        assertTrue(request.contentType!!.startsWith("application/json"), request.contentType)
    }

    @Test
    fun `a denied refund is refused`() = runTest {
        assertFailsWith<PaymentProviderException> { provider { signed("0190", "3") }.refund(reference, 600) }
        assertFailsWith<PaymentProviderException> { provider { answer("""{"errorCode":"SIS0051"}""") }.refund(reference, 600) }
    }

    @Test
    fun `an answer that is not signed by Redsys is not trusted`() = runTest {
        assertFailsWith<PaymentProviderException> { provider { signed("0900", "3", signKey = "another key of 16+ chars") }.refund(reference, 600) }
        assertFailsWith<PaymentProviderException> { provider { answer("not json") }.refund(reference, 600) }
    }

    @Test
    fun `a cancellation is a type 9 request`() = runTest {
        val sent = mutableListOf<Sent>()

        val result = provider(sent) { signed("0400", "9") }.cancel(reference, 600)

        assertEquals(PaymentStatus.CANCELLED, result.status)
        assertEquals(listOf("9"), sent.map { it.type })
    }

    @Test
    fun `a payment that cannot be voided is refunded instead`() = runTest {
        val sent = mutableListOf<Sent>()

        val result = provider(sent) { request ->
            if (request.type == "9") answer("""{"errorCode":"SIS0057"}""") else signed("0900", "3")
        }.cancel(reference, 600)

        assertEquals(PaymentStatus.REFUNDED, result.status)
        assertEquals(listOf("9", "3"), sent.map { it.type })
    }

    @Test
    fun `a cancellation that is neither voided nor refunded is refused`() = runTest {
        assertFailsWith<PaymentProviderException> { provider { answer("""{"errorCode":"SIS0057"}""") }.cancel(reference, 600) }
    }

    @Test
    fun `redsys being down is a transient problem, not a refusal`() = runTest {
        val sent = mutableListOf<Sent>()

        assertFailsWith<PaymentGatewayUnavailableException> { provider(sent) { answer("oops", HttpStatusCode.BadGateway) }.refund(reference, 600) }
        // Not worth trying to refund instead, when cancelling failed because it is down
        assertFailsWith<PaymentGatewayUnavailableException> { provider(sent) { answer("oops", HttpStatusCode.ServiceUnavailable) }.cancel(reference, 600) }
        assertEquals(listOf("3", "9"), sent.map { it.type })
    }

    @Test
    fun `no connection is a transient problem`() = runTest {
        val provider = RedsysPaymentProvider(
            RedsysConfig("999008881", "1", key),
            httpClient = HttpClient(MockEngine { throw IOException("connection refused") }),
        )

        assertFailsWith<PaymentGatewayUnavailableException> { provider.refund(reference, 600) }
    }

    @Test
    fun `the amount must be positive`() = runTest {
        assertFailsWith<IllegalArgumentException> { provider { signed("0900", "3") }.refund(reference, 0) }
    }
}
