package org.centrexcursionistalcoi.app.payment

import io.ktor.client.request.setBody
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.client.plugins.resources.post
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.PaymentStatus
import org.centrexcursionistalcoi.app.data.RedsysPayload
import org.centrexcursionistalcoi.app.data.RedsysReference
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.table.PaymentAuditLog
import org.centrexcursionistalcoi.app.payment.redsys.RedsysConfig
import org.centrexcursionistalcoi.app.payment.redsys.RedsysPaymentProvider
import org.centrexcursionistalcoi.app.routes.Api
import org.jetbrains.exposed.v1.jdbc.selectAll

class TestPaymentAudit : ApplicationTestBase() {
    @AfterTest
    fun cleanup() = PaymentProviders.clear()

    private fun register() =
        PaymentProviders.register(RedsysPaymentProvider(RedsysConfig("999008881", "1", RedsysConfig.SANDBOX_KEY)))

    private suspend fun start(): PaymentTransactions.Started {
        register()
        return PaymentTransactions.start(
            PaymentRequest(amountCents = 600, description = "Night", successUrl = "https://ok", failureUrl = "https://ko"),
            actor = PaymentAudit.userActor("user-1"),
        )
    }

    private fun resultOf(started: PaymentTransactions.Started, status: PaymentStatus) = PaymentResult(
        started.session.reference, status, 600,
        RedsysPayload(
            (started.session.reference as RedsysReference).order, "0000", "600", "978", "1", "1", "0",
            cardNumber = "4548812049400004",
        ),
    )

    @Test
    fun `starting a payment is recorded with who started it and the amount`() = runApplicationTest {
        val started = start()

        val entries = PaymentAudit.forTransaction(started.transactionId)
        assertEquals(listOf(PaymentAuditEvent.CREATED), entries.map { it.event })
        val entry = entries.single()
        assertEquals("user:user-1", entry.actor)
        assertEquals(600, entry.amountCents)
        assertEquals(PaymentStatus.PENDING, entry.newStatus)
        assertEquals(started.session.reference.key, entry.referenceKey)
        assertEquals("redsys", entry.provider)
    }

    @Test
    fun `what the provider reports and the status change are recorded, with the card censored`() = runApplicationTest {
        val started = start()

        PaymentTransactions.apply(resultOf(started, PaymentStatus.COMPLETED))

        val events = PaymentAudit.forTransaction(started.transactionId)
        assertEquals(
            listOf(PaymentAuditEvent.CREATED, PaymentAuditEvent.RESULT_RECEIVED, PaymentAuditEvent.STATUS_CHANGED),
            events.map { it.event },
        )
        val changed = events.last()
        assertEquals(PaymentStatus.PENDING, changed.previousStatus)
        assertEquals(PaymentStatus.COMPLETED, changed.newStatus)
        assertEquals(PaymentAudit.ACTOR_PROVIDER, changed.actor)
        val detail = assertNotNull(events[1].detail).toString()
        assertTrue("****0004" in detail, detail)
        assertTrue("4548812049400004" !in detail, "The full card number must not be kept")
    }

    @Test
    fun `a repeated report is recorded but ignored`() = runApplicationTest {
        val started = start()
        PaymentTransactions.apply(resultOf(started, PaymentStatus.COMPLETED))

        PaymentTransactions.apply(resultOf(started, PaymentStatus.FAILED))

        val last = PaymentAudit.forTransaction(started.transactionId).takeLast(2)
        assertEquals(listOf(PaymentAuditEvent.RESULT_RECEIVED, PaymentAuditEvent.RESULT_IGNORED), last.map { it.event })
        assertEquals(PaymentStatus.FAILED, last[0].newStatus)
        assertEquals(PaymentStatus.COMPLETED, last[1].newStatus)
    }

    @Test
    fun `a report about an unknown payment is traceable by its reference`() = runApplicationTest {
        val unknown = RedsysReference("9999ABCDEFGH")
        val result = PaymentResult(unknown, PaymentStatus.COMPLETED, 600, RedsysPayload("9999ABCDEFGH", "0000", "600", "978", "1", "1", "0"))

        assertNull(PaymentTransactions.apply(result))

        val entry = PaymentAudit.forReference(unknown.key).single()
        assertEquals(PaymentAuditEvent.UNKNOWN_REFERENCE, entry.event)
        assertNull(entry.transactionId)
    }

    @Test
    fun `a rejected notification is recorded`() = runApplicationTest {
        register()

        client.post(Api.Payments.RedsysNotification()) {
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("Ds_SignatureVersion", "HMAC_SHA512_V2")
                        append("Ds_MerchantParameters", "e30")
                        append("Ds_Signature", "nope")
                    }
                )
            )
        }.assertStatusCode(HttpStatusCode.BadRequest)

        val entry = Database { PaymentAuditLog.selectAll().single() }
        assertEquals(PaymentAuditEvent.NOTIFICATION_REJECTED, entry[PaymentAuditLog.event])
        assertEquals("redsys", entry[PaymentAuditLog.provider])
    }

    @Test
    fun `a failing listener is recorded`() = runApplicationTest {
        val started = start()
        PaymentTransactions.listen("broken") { error("boom") }
        try {
            PaymentTransactions.apply(resultOf(started, PaymentStatus.COMPLETED))
        } finally {
            PaymentTransactions.listen("broken") { }
        }

        val failed = PaymentAudit.forTransaction(started.transactionId).single { it.event == PaymentAuditEvent.LISTENER_FAILED }
        assertTrue("broken" in failed.detail.toString())
    }

    @Test
    fun `a refund asked for by a user is traced to them`() = runApplicationTest {
        val started = start()
        PaymentTransactions.apply(resultOf(started, PaymentStatus.COMPLETED))
        val provider = PaymentProviders.of(started.session.reference)
        PaymentProviders.register(object : PaymentProvider by provider {
            override suspend fun refund(reference: org.centrexcursionistalcoi.app.data.PaymentReference, amountCents: Int) =
                resultOf(started, PaymentStatus.REFUNDED)
        })

        PaymentTransactions.refund(started.transactionId, PaymentAudit.userActor("manager"))

        val events = PaymentAudit.forTransaction(started.transactionId)
        val requested = events.single { it.event == PaymentAuditEvent.REFUND_REQUESTED }
        assertEquals("user:manager", requested.actor)
        assertEquals(PaymentStatus.COMPLETED, requested.previousStatus)
        assertEquals(PaymentStatus.REFUNDED, events.last().newStatus)
    }

    @Test
    fun `a refund the provider cannot make is recorded as failed`() = runApplicationTest {
        val started = start()
        PaymentTransactions.apply(resultOf(started, PaymentStatus.COMPLETED))
        val provider = PaymentProviders.of(started.session.reference)
        PaymentProviders.register(object : PaymentProvider by provider {
            override suspend fun refund(reference: org.centrexcursionistalcoi.app.data.PaymentReference, amountCents: Int): PaymentResult =
                throw PaymentGatewayUnavailableException("down")
        })

        try {
            PaymentTransactions.refund(started.transactionId, PaymentAudit.userActor("manager"))
        } catch (_: PaymentGatewayUnavailableException) {
        }

        val events = PaymentAudit.forTransaction(started.transactionId).map { it.event }
        assertTrue(PaymentAuditEvent.REFUND_REQUESTED in events)
        assertEquals(PaymentAuditEvent.OPERATION_FAILED, events.last())
    }
}
