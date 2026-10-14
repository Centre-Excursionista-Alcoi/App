package org.centrexcursionistalcoi.app.plugins

import org.centrexcursionistalcoi.app.data.PaymentAuditEvent
import org.centrexcursionistalcoi.app.translation.locale
import org.centrexcursionistalcoi.app.translation.DocumentRedactor
import org.centrexcursionistalcoi.app.routes.WebTemplate
import org.centrexcursionistalcoi.app.payment.PaymentSession
import org.centrexcursionistalcoi.app.payment.PaymentLaunches
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import kotlinx.html.title
import kotlinx.html.script
import kotlinx.html.p
import kotlinx.html.meta
import kotlinx.html.link
import kotlinx.html.input
import kotlinx.html.img
import kotlinx.html.id
import kotlinx.html.head
import kotlinx.html.h1
import kotlinx.html.form
import kotlinx.html.div
import kotlinx.html.button
import kotlinx.html.body
import kotlinx.html.InputType
import kotlinx.html.FormMethod
import kotlinx.html.ButtonType
import io.ktor.server.response.respondText
import io.ktor.server.response.respondRedirect
import io.ktor.server.resources.get
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveParameters
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.centrexcursionistalcoi.app.data.RedsysReference
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.payment.InvalidPaymentNotificationException
import org.centrexcursionistalcoi.app.payment.PaymentAudit
import org.centrexcursionistalcoi.app.payment.PaymentGatewayUnavailableException
import org.centrexcursionistalcoi.app.payment.PaymentProviders
import org.centrexcursionistalcoi.app.payment.PaymentTransactions
import org.centrexcursionistalcoi.app.payment.RawPaymentNotification
import org.centrexcursionistalcoi.app.routes.Api
import org.centrexcursionistalcoi.app.security.UserSession.Companion.assertAdmin
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid

private val logger = LoggerFactory.getLogger("PaymentRoutes")

/**
 * The page the app opens in a browser to send the user to pay. A provider whose payment is a form (Redsys) needs a
 * `POST` the app can't make, so the form is on this page and submitted by `payment-launch.js`; the button is there
 * for when JavaScript is off.
 */
@OptIn(ExperimentalXmlUtilApi::class)
private object PaymentLaunchPage : WebTemplate("pay") {
    override fun DocumentRedactor.render(args: Map<String, String?>): String = html {
        head {
            meta(charset = "utf-8")
            meta(name = "viewport", content = "width=device-width, initial-scale=1")
            meta(name = "robots", content = "noindex")
            title { +t("title") }
            link(rel = "stylesheet", href = "/static/app-links.css")
            link(rel = "stylesheet", href = "/static/verify.css")
        }
        body {
            div("card") {
                img(classes = "icon", src = "/static/app-icon.png", alt = "")
                val url = args["url"]
                if (url == null) {
                    h1 { tr("expired_title") }
                    p("result error") {
                        attributes["role"] = "status"
                        tr("expired")
                    }
                } else {
                    h1 { tr("title") }
                    p { tr("redirecting") }
                    form(action = url, method = FormMethod.post) {
                        id = "pay"
                        for ((name, value) in launchFields(args)) {
                            input(type = InputType.hidden, name = name) { this.value = value }
                        }
                        button(type = ButtonType.submit, classes = "cta") { tr("button") }
                    }
                    script(src = "/static/payment-launch.js") { defer = true }
                }
            }
        }
    }

    private fun launchFields(args: Map<String, String?>): List<Pair<String, String>> =
        args.filterKeys { it.startsWith(FIELD_PREFIX) }.mapNotNull { (key, value) -> value?.let { key.removePrefix(FIELD_PREFIX) to it } }

    val FIELD_PREFIX = "field:"
}

/**
 * What payment providers call. Public: a notification is trusted by its signature, never by who sent it.
 */
fun Route.paymentRoutes() {
    get<Api.PaymentLaunch> { resource ->
        val launch = resource.id?.let { PaymentLaunches[it] }
        when (val session = launch?.session) {
            is PaymentSession.Redirect -> call.respondRedirect(session.url)
            is PaymentSession.Form -> call.respondText(
                PaymentLaunchPage.render(
                    call.request.locale(),
                    mapOf("url" to session.url) + session.fields.mapKeys { PaymentLaunchPage.FIELD_PREFIX + it.key },
                ),
                ContentType.Text.Html,
            )
            null -> call.respondText(PaymentLaunchPage.render(call.request.locale(), emptyMap()), ContentType.Text.Html, HttpStatusCode.NotFound)
        }
    }

    post<Api.Payments.RedsysNotification> {
        val provider = PaymentProviders[RedsysReference.PROVIDER]
            ?: throw PaymentGatewayUnavailableException("Redsys is not configured")
        val parameters = call.receiveParameters().entries().associate { (name, values) -> name to values.first() }

        val result = try {
            provider.handleNotification(RawPaymentNotification(parameters = parameters))
        } catch (e: InvalidPaymentNotificationException) {
            logger.warn("Rejected a Redsys notification: ${e.message}")
            PaymentAudit.tryRecord(
                PaymentAuditEvent.NOTIFICATION_REJECTED, PaymentAudit.ACTOR_PROVIDER,
                provider = RedsysReference.PROVIDER, detail = PaymentAudit.errorDetail(e),
            )
            return@post respondError(Error.MalformedRequest())
        }

        val applied = PaymentTransactions.apply(result)
        if (applied == null) {
            logger.warn("Redsys notified a payment with no transaction: ${result.reference.key}")
            return@post respondError(Error.EntityNotFound("Transaction", result.reference.key))
        }
        logger.info("Transaction ${applied.transactionId}: ${applied.previous} -> ${applied.current}")
        call.respond(HttpStatusCode.OK)
    }

    // Admins only: the trace of everything that happened with the gateway, for support and legal questions
    get<Api.Payments.Audit> { resource ->
        assertAdmin() ?: return@get
        val limit = (resource.limit ?: DEFAULT_AUDIT_LIMIT).coerceIn(1, MAX_AUDIT_LIMIT)
        val entries = when {
            resource.transaction != null -> {
                val id = runCatching { Uuid.parse(resource.transaction!!) }.getOrNull()
                    ?: return@get respondError(Error.InvalidArgument("transaction"))
                PaymentAudit.forTransaction(id)
            }
            resource.reference != null -> PaymentAudit.forReference(resource.reference!!)
            else -> PaymentAudit.latest(limit)
        }
        call.respond(entries.takeLast(limit))
    }
}

private const val DEFAULT_AUDIT_LIMIT = 100
private const val MAX_AUDIT_LIMIT = 1000
