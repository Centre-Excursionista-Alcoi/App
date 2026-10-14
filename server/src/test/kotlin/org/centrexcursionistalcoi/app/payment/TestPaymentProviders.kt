package org.centrexcursionistalcoi.app.payment

import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import org.centrexcursionistalcoi.app.data.PaymentMethod
import org.centrexcursionistalcoi.app.data.PaymentPayload
import org.centrexcursionistalcoi.app.data.PaymentReference
import org.centrexcursionistalcoi.app.data.PaymentStatus
import org.centrexcursionistalcoi.app.data.RedsysPayload
import org.centrexcursionistalcoi.app.data.RedsysReference
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.plugins.configureStatusPages

class TestPaymentProviders {
    private class FakeProvider(override val id: String = RedsysReference.PROVIDER) : PaymentProvider {
        override val methods = setOf(PaymentMethod.CARD)
        private val payload: PaymentPayload = RedsysPayload("1234", "0000", "100", "978", "1", "1", "0")

        override suspend fun createPayment(request: PaymentRequest): PaymentSession =
            PaymentSession.Redirect(RedsysReference("1234"), "https://pay.example/1234")

        override suspend fun handleNotification(notification: RawPaymentNotification) =
            PaymentResult(RedsysReference("1234"), PaymentStatus.COMPLETED, 100, payload)

        override suspend fun refund(reference: PaymentReference, amountCents: Int?): PaymentResult {
            requireOwn(reference)
            return PaymentResult(reference, PaymentStatus.REFUNDED, amountCents, payload)
        }

        override suspend fun cancel(reference: PaymentReference): PaymentResult {
            requireOwn(reference)
            return PaymentResult(reference, PaymentStatus.CANCELLED, null, payload)
        }
    }

    @AfterTest
    fun cleanup() = PaymentProviders.clear()

    @Test
    fun `nothing configured is unavailable, not a crash`() {
        assertFailsWith<PaymentGatewayUnavailableException> { PaymentProviders.default() }
        assertFailsWith<PaymentGatewayUnavailableException> { PaymentProviders.of(RedsysReference("1234")) }
    }

    @Test
    fun `a registered provider is found by id and by reference`() {
        val provider = FakeProvider()
        PaymentProviders.register(provider)

        assertSame(provider, PaymentProviders["redsys"])
        assertSame(provider, PaymentProviders.of(RedsysReference("1234")))
        assertSame(provider, PaymentProviders.default())
    }

    @Test
    fun `a provider rejects references of another`() = kotlinx.coroutines.test.runTest {
        val other = FakeProvider(id = "other")
        assertFailsWith<PaymentProviderException> { other.refund(RedsysReference("1234")) }
        assertFailsWith<PaymentProviderException> { other.cancel(RedsysReference("1234")) }
    }

    @Test
    fun `the amount of a request must be positive`() {
        assertFailsWith<IllegalArgumentException> { PaymentRequest(0, "x", "https://ok", "https://ko") }
    }

    @Test
    fun `an unavailable gateway is answered with a 503 the app can recognise`() = testApplication {
        application {
            install(ContentNegotiation) { json(json) }
            configureStatusPages()
            routing { get("/pay") { PaymentProviders.default() } }
        }

        val response = client.get("/pay")

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertEquals(Error.ERROR_PAYMENT_GATEWAY_UNAVAILABLE.toString(), response.headers["CEA-Error-Code"])
        assertEquals("60", response.headers[HttpHeaders.RetryAfter])
    }
}
