package org.centrexcursionistalcoi.app.payment

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get as getUrl
import io.ktor.client.request.post as postUrl
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.headersOf
import java.util.Base64
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.centrexcursionistalcoi.app.data.PaymentAuditEntry
import org.centrexcursionistalcoi.app.data.PaymentAuditEvent
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.SPACE_LENDINGS_MANAGER_GROUP_NAME
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.Category
import org.centrexcursionistalcoi.app.data.CategoryPrice
import org.centrexcursionistalcoi.app.data.ManualPaymentMethod
import org.centrexcursionistalcoi.app.data.PaymentLaunch
import org.centrexcursionistalcoi.app.data.PaymentMethod
import org.centrexcursionistalcoi.app.data.PaymentStatus
import org.centrexcursionistalcoi.app.data.PriceUnit
import org.centrexcursionistalcoi.app.data.RedsysReference
import org.centrexcursionistalcoi.app.data.ServerInfo
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.SpaceEntity
import org.centrexcursionistalcoi.app.database.entity.SpaceLendingEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.payment.redsys.RedsysConfig
import org.centrexcursionistalcoi.app.payment.redsys.RedsysPaymentProvider
import org.centrexcursionistalcoi.app.payment.redsys.RedsysSignature
import org.centrexcursionistalcoi.app.routes.Api
import org.centrexcursionistalcoi.app.test.LoginType
import org.centrexcursionistalcoi.app.utils.toUuid
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.centrexcursionistalcoi.app.database.table.Transactions

class TestSpaceLendingPayments : ApplicationTestBase() {
    private val key = RedsysConfig.SANDBOX_KEY
    private val spaceId = "5c1c3f0e-0b0a-4c3a-9d55-0a1b2c3d4e5f".toUuid()
    private val today = LocalDate(2026, 10, 1)

    @AfterTest
    fun cleanup() = PaymentProviders.clear()

    context(_: JdbcTransaction)
    private fun createSpace() {
        SpaceEntity.new(spaceId) {
            name = "Casa"
            description = "A house"
            requiresKeys = false
            isClosed = false
            prices = listOf(CategoryPrice(Category.MEMBER, 3.0, PriceUnit.PER_NIGHT))
        }
    }

    private fun register(bizum: Boolean = false, asked: MutableList<String> = mutableListOf(), refundResponse: String = "0900") {
        PaymentProviders.register(
            RedsysPaymentProvider(
                RedsysConfig("999008881", "1", key, bizum = bizum),
                httpClient = HttpClient(MockEngine { request ->
                    val body = Json.parseToJsonElement(String(request.body.toByteArray())).jsonObject
                    val encoded = body.getValue("Ds_MerchantParameters").jsonPrimitive.content
                    val sent = Json.parseToJsonElement(String(Base64.getUrlDecoder().decode(encoded))).jsonObject
                    val type = sent.getValue("DS_MERCHANT_TRANSACTIONTYPE").jsonPrimitive.content
                    val order = sent.getValue("DS_MERCHANT_ORDER").jsonPrimitive.content
                    asked += "$type:${sent.getValue("DS_MERCHANT_AMOUNT").jsonPrimitive.content}"
                    val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                        """{"Ds_Order":"$order","Ds_Response":"$refundResponse","Ds_Amount":"600","Ds_Currency":"978","Ds_MerchantCode":"999008881","Ds_Terminal":"1","Ds_TransactionType":"$type"}""".toByteArray()
                    )
                    respond(
                        """{"Ds_SignatureVersion":"HMAC_SHA512_V2","Ds_MerchantParameters":"$payload","Ds_Signature":"${RedsysSignature.sign(key, order, payload)}"}""",
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }),
            )
        )
    }

    private suspend fun HttpClient.book(): String = postUrl("/space_lendings") {
        contentType(ContentType.Application.Json)
        setBody("""{"space":"$spaceId","checkIn":"2026-10-09","checkOut":"2026-10-10","attendees":{"MEMBER":2}}""")
    }.headers["Location"]!!

    /** Books a night for two members (6 €) and has its keys picked up and returned. */
    private suspend fun HttpClient.returnedLending(): String = book().also {
        postUrl("$it/pickup").assertStatusCode(HttpStatusCode.NoContent)
        postUrl("$it/return").assertStatusCode(HttpStatusCode.NoContent)
    }

    private suspend fun HttpClient.pay(location: String, body: String = "{}"): HttpResponse =
        post(Api.SpaceLendings.Id.Pay(Api.SpaceLendings.Id(location.substringAfterLast('/')))) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    private suspend fun HttpClient.launch(response: HttpResponse): HttpResponse {
        val launch = json.decodeFromString(PaymentLaunch.serializer(), response.bodyAsText())
        return get(Api.PaymentLaunch(launch.url.substringAfter("id=")))
    }

    /** What Redsys would tell the server about the payment started by [launchPage]. */
    private suspend fun HttpClient.redsysNotifies(order: String, response: String = "0000"): HttpResponse {
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
            """{"Ds_Order":"$order","Ds_Response":"$response","Ds_Amount":"600","Ds_Currency":"978","Ds_MerchantCode":"999008881","Ds_Terminal":"1","Ds_TransactionType":"0"}""".toByteArray()
        )
        return post(Api.Payments.RedsysNotification()) {
            setBody(FormDataContent(Parameters.build {
                append("Ds_SignatureVersion", RedsysSignature.VERSION)
                append("Ds_MerchantParameters", payload)
                append("Ds_Signature", RedsysSignature.sign(key, order, payload))
            }))
        }
    }

    /** The order of the only transaction. */
    private fun order(): String = Database {
        (Transactions.selectAll().single()[Transactions.reference] as RedsysReference).order
    }

    private fun lendingStatus(location: String): PaymentStatus = Database {
        SpaceLendingEntity[Uuid.parse(location.substringAfterLast('/'))].paymentStatus
    }

    private val manager: JdbcTransaction.(UserReferenceEntity) -> Unit =
        { it.groups = it.groups + SPACE_LENDINGS_MANAGER_GROUP_NAME }

    @Test
    fun `a lending is paid online once its keys are back, and the manager can refund it`() = runApplicationTest(
        shouldLogIn = LoginType.USER, databaseInitBlock = { createSpace() }, userEntityPatches = manager, mockDate = today,
    ) {
        val asked = mutableListOf<String>()
        register(asked = asked)
        val location = client.returnedLending()

        val started = client.pay(location, """{"method":"CARD"}""")
        started.assertStatusCode(HttpStatusCode.OK)
        val page = client.launch(started)
        page.assertStatusCode(HttpStatusCode.OK)
        val html = page.bodyAsText()
        assertContains(html, "https://sis-t.redsys.es:25443/sis/realizarPago")
        assertContains(html, "Ds_Signature")
        assertEquals(PaymentStatus.PENDING, lendingStatus(location))

        client.redsysNotifies(order()).assertStatusCode(HttpStatusCode.OK)
        assertEquals(PaymentStatus.COMPLETED, lendingStatus(location))

        client.post(Api.SpaceLendings.Id.Refund(Api.SpaceLendings.Id(location.substringAfterLast('/')))).assertStatusCode(HttpStatusCode.NoContent)
        assertEquals(PaymentStatus.REFUNDED, lendingStatus(location))
        // What was charged, in cents, is what is returned
        assertEquals(listOf("3:600"), asked)
    }

    @Test
    fun `the price at the time is what is charged`() = runApplicationTest(
        shouldLogIn = LoginType.USER, databaseInitBlock = { createSpace() }, userEntityPatches = manager, mockDate = today,
    ) {
        register()
        val location = client.returnedLending()
        // The space gets more expensive after the lending was made
        Database { SpaceEntity[spaceId].prices = listOf(CategoryPrice(Category.MEMBER, 9.0, PriceUnit.PER_NIGHT)) }

        client.launch(client.pay(location))

        assertEquals(600, Database { Transactions.selectAll().single()[Transactions.amountCents] })
    }

    @Test
    fun `a denied payment leaves the lending to pay, and it can be tried again`() = runApplicationTest(
        shouldLogIn = LoginType.USER, databaseInitBlock = { createSpace() }, userEntityPatches = manager, mockDate = today,
    ) {
        register()
        val location = client.returnedLending()
        client.pay(location).assertStatusCode(HttpStatusCode.OK)

        client.redsysNotifies(order(), response = "0190").assertStatusCode(HttpStatusCode.OK)

        assertEquals(PaymentStatus.PENDING, lendingStatus(location))
        client.pay(location).assertStatusCode(HttpStatusCode.OK)
    }

    @Test
    fun `it cannot be paid before the keys are back, twice or when there is nothing to pay`() = runApplicationTest(
        shouldLogIn = LoginType.USER, databaseInitBlock = { createSpace() }, userEntityPatches = manager, mockDate = today,
    ) {
        register()
        val location = client.book()
        client.pay(location).assertStatusCode(HttpStatusCode.Conflict)

        client.postUrl("$location/pickup").assertStatusCode(HttpStatusCode.NoContent)
        client.postUrl("$location/return").assertStatusCode(HttpStatusCode.NoContent)
        Database { SpaceLendingEntity[Uuid.parse(location.substringAfterLast('/'))].paymentStatus = PaymentStatus.COMPLETED }
        client.pay(location).assertStatusCode(HttpStatusCode.Conflict)
    }

    @Test
    fun `only the owner pays`() = runApplicationTest(
        shouldLogIn = LoginType.USER, databaseInitBlock = { createSpace() }, userEntityPatches = manager, mockDate = today,
    ) {
        register()
        val location = client.returnedLending()
        Database { SpaceLendingEntity[Uuid.parse(location.substringAfterLast('/'))].userSub = null }

        client.pay(location).assertStatusCode(HttpStatusCode.NotFound)
    }

    @Test
    fun `without a payment gateway it is unavailable, not broken`() = runApplicationTest(
        shouldLogIn = LoginType.USER, databaseInitBlock = { createSpace() }, userEntityPatches = manager, mockDate = today,
    ) {
        val location = client.returnedLending()

        client.pay(location).assertStatusCode(HttpStatusCode.ServiceUnavailable)
        assertEquals(emptyList(), client.get(Api.Info()).let { json.decodeFromString(ServerInfo.serializer(), it.bodyAsText()) }.paymentMethods)
    }

    @Test
    fun `bizum is only accepted when the server offers it`() = runApplicationTest(
        shouldLogIn = LoginType.USER, databaseInitBlock = { createSpace() }, userEntityPatches = manager, mockDate = today,
    ) {
        register(bizum = false)
        val location = client.returnedLending()
        client.pay(location, """{"method":"BIZUM"}""").assertStatusCode(HttpStatusCode.BadRequest)
        assertEquals(listOf(PaymentMethod.CARD), client.get(Api.Info()).let { json.decodeFromString(ServerInfo.serializer(), it.bodyAsText()) }.paymentMethods)

        register(bizum = true)
        client.pay(location, """{"method":"BIZUM","phone":"+34700000000"}""").assertStatusCode(HttpStatusCode.OK)
        assertEquals(listOf(PaymentMethod.CARD, PaymentMethod.BIZUM), client.get(Api.Info()).let { json.decodeFromString(ServerInfo.serializer(), it.bodyAsText()) }.paymentMethods)
    }

    @Test
    fun `a lending paid by hand has nothing to refund online`() = runApplicationTest(
        shouldLogIn = LoginType.USER, databaseInitBlock = { createSpace() }, userEntityPatches = manager, mockDate = today,
    ) {
        register()
        val location = client.returnedLending()

        client.post(Api.SpaceLendings.Id.Refund(Api.SpaceLendings.Id(location.substringAfterLast('/')))).assertStatusCode(HttpStatusCode.Conflict)
    }

    private suspend fun HttpClient.setPayment(location: String, body: String): HttpResponse =
        post(Api.SpaceLendings.Id.Payment(Api.SpaceLendings.Id(location.substringAfterLast('/')))) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    private fun manualMethod(location: String): ManualPaymentMethod? = Database {
        SpaceLendingEntity[Uuid.parse(location.substringAfterLast('/'))].manualPaymentMethod
    }

    @Test
    fun `a payment recorded by hand keeps its method, is audited and can be undone`() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN, databaseInitBlock = { createSpace() }, mockDate = today,
    ) {
        val location = client.returnedLending()

        client.setPayment(location, """{"status":"COMPLETED","method":"CASH"}""").assertStatusCode(HttpStatusCode.NoContent)
        assertEquals(PaymentStatus.COMPLETED, lendingStatus(location))
        assertEquals(ManualPaymentMethod.CASH, manualMethod(location))

        client.setPayment(location, """{"status":"PENDING"}""").assertStatusCode(HttpStatusCode.NoContent)
        assertEquals(PaymentStatus.PENDING, lendingStatus(location))
        assertEquals(null, manualMethod(location))

        val audit = client.auditOf(location)
        assertEquals(listOf(PaymentAuditEvent.MANUAL_PAYMENT_SET, PaymentAuditEvent.MANUAL_PAYMENT_UNDONE), audit.map { it.event })
        assertContains(audit.first().actor, "user:")
        assertEquals(PaymentStatus.PENDING, audit.first().previousStatus)
        assertEquals(PaymentStatus.COMPLETED, audit.first().newStatus)
        assertEquals(600, audit.first().amountCents)
        assertEquals("CASH", audit.first().detail!!.getValue("method").jsonPrimitive.content)
        // Undoing says what was undone
        assertEquals("CASH", audit.last().detail!!.getValue("method").jsonPrimitive.content)
        assertEquals(PaymentStatus.COMPLETED, audit.last().previousStatus)
    }

    @Test
    fun `a payment recorded without a method is recorded as other`() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN, databaseInitBlock = { createSpace() }, mockDate = today,
    ) {
        val location = client.returnedLending()

        client.setPayment(location, """{"status":"COMPLETED"}""").assertStatusCode(HttpStatusCode.NoContent)

        assertEquals(ManualPaymentMethod.OTHER, manualMethod(location))
    }

    @Test
    fun `a payment through the gateway cannot be undone, only refunded`() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN, databaseInitBlock = { createSpace() }, mockDate = today,
    ) {
        register()
        val location = client.returnedLending()
        client.pay(location).assertStatusCode(HttpStatusCode.OK)
        client.redsysNotifies(order()).assertStatusCode(HttpStatusCode.OK)

        client.setPayment(location, """{"status":"PENDING"}""").assertStatusCode(HttpStatusCode.Conflict)

        assertEquals(PaymentStatus.COMPLETED, lendingStatus(location))
        assertEquals(null, manualMethod(location))
        client.post(Api.SpaceLendings.Id.Refund(Api.SpaceLendings.Id(location.substringAfterLast('/')))).assertStatusCode(HttpStatusCode.NoContent)
        assertEquals(PaymentStatus.REFUNDED, lendingStatus(location))
    }

    @Test
    fun `a payment cannot be recorded early, twice or with any other status`() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN, databaseInitBlock = { createSpace() }, mockDate = today,
    ) {
        val booked = client.book()
        client.setPayment(booked, """{"status":"COMPLETED","method":"CASH"}""").assertStatusCode(HttpStatusCode.Conflict)

        client.postUrl("$booked/pickup").assertStatusCode(HttpStatusCode.NoContent)
        client.postUrl("$booked/return").assertStatusCode(HttpStatusCode.NoContent)
        val location = booked
        // Nothing to undo yet
        client.setPayment(location, """{"status":"PENDING"}""").assertStatusCode(HttpStatusCode.Conflict)
        client.setPayment(location, """{"status":"REFUNDED"}""").assertStatusCode(HttpStatusCode.Conflict)
        client.setPayment(location, """{"status":"COMPLETED","method":"CASH"}""").assertStatusCode(HttpStatusCode.NoContent)
        client.setPayment(location, """{"status":"COMPLETED","method":"TRANSFER"}""").assertStatusCode(HttpStatusCode.Conflict)

        assertEquals(ManualPaymentMethod.CASH, manualMethod(location))
        assertEquals(1, client.auditOf(location).size)
    }

    @Test
    fun `a refused refund is reported as such and changes nothing`() = runApplicationTest(
        shouldLogIn = LoginType.USER, databaseInitBlock = { createSpace() }, userEntityPatches = manager, mockDate = today,
    ) {
        register(refundResponse = "0190")
        val location = client.returnedLending()
        client.pay(location).assertStatusCode(HttpStatusCode.OK)
        client.redsysNotifies(order()).assertStatusCode(HttpStatusCode.OK)

        client.post(Api.SpaceLendings.Id.Refund(Api.SpaceLendings.Id(location.substringAfterLast('/')))).assertStatusCode(HttpStatusCode.Conflict)

        assertEquals(PaymentStatus.COMPLETED, lendingStatus(location))
    }

    @Test
    fun `an unknown or expired launch is not found`() = runApplicationTest {
        assertEquals(HttpStatusCode.NotFound, client.get(Api.PaymentLaunch("nope")).status)
        assertEquals(HttpStatusCode.NotFound, client.get(Api.PaymentLaunch()).status)
    }

    @Test
    fun `a provider that redirects sends the user there`() = runApplicationTest {
        val launch = PaymentLaunches.create(PaymentSession.Redirect(RedsysReference("1234ABCD"), "https://pay.example/1234"))

        val client = createClient { followRedirects = false }
        val response = client.getUrl("/pay?id=${launch.id}")

        assertEquals(HttpStatusCode.Found, response.status)
        assertEquals("https://pay.example/1234", response.headers[HttpHeaders.Location])
        assertNotNull(PaymentLaunches[launch.id])
    }

    private suspend fun HttpClient.auditOf(location: String): List<PaymentAuditEntry> {
        val response = get(Api.SpaceLendings.Id.PaymentAudit(Api.SpaceLendings.Id(location.substringAfterLast('/'))))
        response.assertStatusCode(HttpStatusCode.OK)
        return json.decodeFromString(ListSerializer(PaymentAuditEntry.serializer()), response.bodyAsText())
    }

    @Test
    fun `an admin sees the audit log of a lending, and the whole gateway's`() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN, databaseInitBlock = { createSpace() }, mockDate = today,
    ) {
        register()
        val location = client.returnedLending()
        client.pay(location).assertStatusCode(HttpStatusCode.OK)
        client.redsysNotifies(order()).assertStatusCode(HttpStatusCode.OK)

        val events = client.auditOf(location).map { it.event }
        assertEquals(
            listOf(PaymentAuditEvent.CREATED, PaymentAuditEvent.RESULT_RECEIVED, PaymentAuditEvent.STATUS_CHANGED),
            events,
        )
        // The creation names who paid
        assertContains(client.auditOf(location).first().actor, "user:")

        val all = client.get(Api.Payments.Audit(limit = 2))
        all.assertStatusCode(HttpStatusCode.OK)
        assertEquals(2, json.decodeFromString(ListSerializer(PaymentAuditEntry.serializer()), all.bodyAsText()).size)
        val byReference = client.get(Api.Payments.Audit(reference = "redsys:${order()}"))
        assertEquals(3, json.decodeFromString(ListSerializer(PaymentAuditEntry.serializer()), byReference.bodyAsText()).size)
    }

    @Test
    fun `a manager who is not an admin cannot read the audit log`() = runApplicationTest(
        shouldLogIn = LoginType.USER, databaseInitBlock = { createSpace() }, userEntityPatches = manager, mockDate = today,
    ) {
        register()
        val location = client.returnedLending()

        client.get(Api.SpaceLendings.Id.PaymentAudit(Api.SpaceLendings.Id(location.substringAfterLast('/'))))
            .assertStatusCode(HttpStatusCode.Forbidden)
        client.get(Api.Payments.Audit()).assertStatusCode(HttpStatusCode.Forbidden)
    }

    @Test
    fun `the audit log rejects a malformed transaction id`() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        client.get(Api.Payments.Audit(transaction = "nope")).assertStatusCode(HttpStatusCode.BadRequest)
    }
}
