package org.centrexcursionistalcoi.app.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.resources.Resources
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.mockk
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.data.PaymentAuditEvent
import org.centrexcursionistalcoi.app.data.PaymentMethod
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.settings.SettingsStore
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class TestSpaceLendingsRemoteRepositoryPayments {
    private val original = _httpClient
    private val lendingId = Uuid.random()

    @BeforeTest
    fun setUp() {
        startKoin { modules(module { single { mockk<SettingsStore>(relaxed = true) } }) }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        _httpClient = original
    }

    private fun repository() = SpaceLendingsRemoteRepository(mockk(relaxed = true))

    @Test
    fun `starting a payment posts the method and reads where to open`() = runTest {
        val seen = mutableListOf<Triple<HttpMethod, String, String>>()
        _httpClient = HttpClient(MockEngine { request ->
            seen += Triple(request.method, request.url.encodedPath, String(request.body.toByteArray()))
            respond(
                """{"url":"https://app.example/pay?id=abc","expires_in":1800}""",
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }) { install(Resources) }

        val launch = repository().startPayment(lendingId, PaymentMethod.BIZUM, "+34700000000")

        assertEquals("https://app.example/pay?id=abc", launch.url)
        assertEquals(1800, launch.expiresIn)
        val (method, path, body) = seen.single()
        assertEquals(HttpMethod.Post, method)
        assertEquals("/space_lendings/$lendingId/pay", path)
        assertEquals("""{"method":"BIZUM","phone":"+34700000000"}""", body)
    }

    @Test
    fun `an unavailable gateway reaches the caller with its code`() = runTest {
        _httpClient = HttpClient(MockEngine {
            respond(
                json.encodeToString(Error.serializer(), Error.PaymentGatewayUnavailable()),
                HttpStatusCode.ServiceUnavailable,
                headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }) { install(Resources) }

        val error = assertFailsWith<ServerException> { repository().startPayment(lendingId) }

        assertEquals(Error.ERROR_PAYMENT_GATEWAY_UNAVAILABLE, error.errorCode)
        assertEquals(503, error.responseStatusCode)
    }

    @Test
    fun `a refused refund reaches the caller with its code`() = runTest {
        _httpClient = HttpClient(MockEngine {
            respond(
                json.encodeToString(Error.serializer(), Error.PaymentRejected("0190")),
                HttpStatusCode.Conflict,
                headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }) { install(Resources) }

        val error = assertFailsWith<ServerException> { repository().refund(lendingId) }

        assertEquals(Error.ERROR_PAYMENT_REJECTED, error.errorCode)
    }

    @Test
    fun `the audit log of a lending is read from its endpoint`() = runTest {
        val seen = mutableListOf<String>()
        _httpClient = HttpClient(MockEngine { request ->
            seen += request.url.encodedPath
            respond(
                """[{"id":"${Uuid.random()}","createdAt":1760000000000,"transactionId":null,"referenceKey":"redsys:1","provider":"redsys","event":"CREATED","actor":"user:a","previousStatus":null,"newStatus":"PENDING","amountCents":600,"detail":null}]""",
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }) { install(Resources) }

        val entries = repository().paymentAudit(lendingId)

        assertEquals("/space_lendings/$lendingId/payment_audit", seen.single())
        assertEquals(PaymentAuditEvent.CREATED, entries.single().event)
        assertEquals(600, entries.single().amountCents)
    }
}
