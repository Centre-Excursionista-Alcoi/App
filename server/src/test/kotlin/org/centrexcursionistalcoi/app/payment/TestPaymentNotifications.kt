package org.centrexcursionistalcoi.app.payment

import io.ktor.client.HttpClient
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.client.request.forms.FormDataContent
import java.util.Base64
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.PaymentStatus
import org.centrexcursionistalcoi.app.data.RedsysPayload
import org.centrexcursionistalcoi.app.data.RedsysReference
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.table.Transactions
import org.centrexcursionistalcoi.app.payment.redsys.RedsysConfig
import org.centrexcursionistalcoi.app.payment.redsys.RedsysPaymentProvider
import org.centrexcursionistalcoi.app.payment.redsys.RedsysSignature
import org.centrexcursionistalcoi.app.routes.Api
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

class TestPaymentNotifications : ApplicationTestBase() {
    private val key = RedsysConfig.SANDBOX_KEY

    @AfterTest
    fun cleanup() = PaymentProviders.clear()

    private fun register() = PaymentProviders.register(RedsysPaymentProvider(RedsysConfig("999008881", "1", key)))

    private suspend fun start(): PaymentTransactions.Started {
        register()
        return PaymentTransactions.start(
            PaymentRequest(amountCents = 600, description = "Night", successUrl = "https://app.example/ok", failureUrl = "https://app.example/ko")
        )
    }

    private suspend fun HttpClient.notify(
        order: String,
        response: String = "0000",
        type: String = "0",
        signKey: String = key,
    ): HttpResponse {
        val json = """{"Ds_Order":"$order","Ds_Response":"$response","Ds_Amount":"600","Ds_Currency":"978","Ds_MerchantCode":"999008881","Ds_Terminal":"1","Ds_TransactionType":"$type","Ds_Card_Number":"454881******0004"}"""
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray())
        return post(Api.Payments.RedsysNotification()) {
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("Ds_SignatureVersion", RedsysSignature.VERSION)
                        append("Ds_MerchantParameters", encoded)
                        append("Ds_Signature", RedsysSignature.sign(signKey, order, encoded))
                    }
                )
            )
        }
    }

    private fun row(id: Uuid) = Database { Transactions.selectAll().where { Transactions.id eq id }.single() }

    private fun orderOf(started: PaymentTransactions.Started) = assertIs<RedsysReference>(started.session.reference).order

    @Test
    fun `starting a payment records it as pending, with the amount agreed`() = runApplicationTest {
        val started = start()

        val row = row(started.transactionId)
        assertEquals("redsys", row[Transactions.provider])
        assertEquals(PaymentStatus.PENDING, row[Transactions.status])
        assertEquals(600, row[Transactions.amountCents])
        assertEquals(started.session.reference, row[Transactions.reference])
        assertEquals(started.session.reference.key, row[Transactions.referenceKey])
    }

    @Test
    fun `starting a payment without a provider is unavailable`() = runApplicationTest {
        try {
            PaymentTransactions.start(PaymentRequest(100, "x", "https://ok", "https://ko"))
            error("Expected the gateway to be unavailable")
        } catch (_: PaymentGatewayUnavailableException) {
        }
    }

    @Test
    fun `an authorized notification completes the payment and the card is censored`() = runApplicationTest {
        val started = start()

        client.notify(orderOf(started)).assertStatusCode(HttpStatusCode.OK)

        val row = row(started.transactionId)
        assertEquals(PaymentStatus.COMPLETED, row[Transactions.status])
        assertEquals("****0004", assertIs<RedsysPayload>(row[Transactions.payload]).cardNumber)
    }

    @Test
    fun `a denied notification fails the payment`() = runApplicationTest {
        val started = start()

        client.notify(orderOf(started), response = "0190").assertStatusCode(HttpStatusCode.OK)

        assertEquals(PaymentStatus.FAILED, row(started.transactionId)[Transactions.status])
    }

    @Test
    fun `repeated or late notifications change nothing`() = runApplicationTest {
        val started = start()
        val order = orderOf(started)
        client.notify(order).assertStatusCode(HttpStatusCode.OK)

        // Again, and a failure after the money was taken
        client.notify(order).assertStatusCode(HttpStatusCode.OK)
        client.notify(order, response = "0190").assertStatusCode(HttpStatusCode.OK)

        assertEquals(PaymentStatus.COMPLETED, row(started.transactionId)[Transactions.status])
        assertFalse(PaymentTransactions.apply(
            PaymentResult(started.session.reference, PaymentStatus.COMPLETED, 600, RedsysPayload(order, "0000", "600", "978", "1", "1", "0"))
        )!!.changed)
    }

    @Test
    fun `a refund can follow a completed payment`() = runApplicationTest {
        val started = start()
        val order = orderOf(started)
        client.notify(order).assertStatusCode(HttpStatusCode.OK)

        client.notify(order, response = "0900", type = "3").assertStatusCode(HttpStatusCode.OK)

        assertEquals(PaymentStatus.REFUNDED, row(started.transactionId)[Transactions.status])
    }

    @Test
    fun `a notification with a wrong signature is rejected and changes nothing`() = runApplicationTest {
        val started = start()

        val response = client.notify(orderOf(started), signKey = "another key of 16+ chars")

        response.assertStatusCode(HttpStatusCode.BadRequest)
        assertEquals(PaymentStatus.PENDING, row(started.transactionId)[Transactions.status])
    }

    @Test
    fun `a signed notification for an unknown payment is not found`() = runApplicationTest {
        register()

        client.notify("0000ZZZZ").assertStatusCode(HttpStatusCode.NotFound)
    }

    @Test
    fun `notifications while the gateway is not configured are answered with 503`() = runApplicationTest {
        client.notify("1234ABCD").assertStatusCode(HttpStatusCode.ServiceUnavailable)
        assertTrue(PaymentProviders["redsys"] == null)
    }
}
