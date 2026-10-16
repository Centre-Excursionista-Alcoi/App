package org.centrexcursionistalcoi.app.plugins

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.request.receiveNullable
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.sse.sse
import kotlinx.html.ButtonType
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.code
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.img
import kotlinx.html.link
import kotlinx.html.meta
import kotlinx.html.noScript
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.title
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import org.centrexcursionistalcoi.app.AppLinks
import org.centrexcursionistalcoi.app.data.BROWSER_LOGIN_APPROVED_EVENT
import org.centrexcursionistalcoi.app.data.BROWSER_LOGIN_EXPIRED_EVENT
import org.centrexcursionistalcoi.app.data.BROWSER_LOGIN_SECRET_HEADER
import org.centrexcursionistalcoi.app.data.BrowserLoginRequest
import org.centrexcursionistalcoi.app.data.BrowserLoginStartRequest
import org.centrexcursionistalcoi.app.data.RestoreKeyVerificationRequest
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.table.AuthEventType
import org.centrexcursionistalcoi.app.database.table.AuthSessionMethod
import org.centrexcursionistalcoi.app.database.table.AuthSessionRevocationReason
import org.centrexcursionistalcoi.app.database.table.CredentialKind
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.routes.Api
import org.centrexcursionistalcoi.app.routes.WebTemplate
import org.centrexcursionistalcoi.app.security.AssertionVerification
import org.centrexcursionistalcoi.app.security.AuthTokens
import org.centrexcursionistalcoi.app.security.BrowserLogins
import org.centrexcursionistalcoi.app.security.ClientInfo
import org.centrexcursionistalcoi.app.security.verifyAssertion
import org.centrexcursionistalcoi.app.translation.DocumentRedactor
import org.centrexcursionistalcoi.app.translation.locale
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("BrowserLogin")

/**
 * The page the user opens in their browser to finish a sign-in started by an app, see [BrowserLogins].
 *
 * The button is hidden until `browser-login.js` has checked the browser can use passkeys, so a browser without
 * JavaScript (the `<noscript>` message) or without passkey support (the `#unsupported` message) never shows a button
 * that does nothing.
 */
@OptIn(ExperimentalXmlUtilApi::class)
private object BrowserLoginPage : WebTemplate("browser_login") {
    override fun DocumentRedactor.render(args: Map<String, String?>): String = html {
        head {
            meta(charset = "utf-8")
            meta(name = "viewport", content = "width=device-width, initial-scale=1")
            meta(name = "robots", content = "noindex")
            title { +t("title") }
            link(rel = "stylesheet", href = "/static/app-links.css")
            link(rel = "stylesheet", href = "/static/verify.css")
            link(rel = "stylesheet", href = "/static/browser-login.css")
        }
        body {
            div("card") {
                img(classes = "icon", src = "/static/app-icon.png", alt = "")
                val id = args["id"]
                if (id == null) {
                    h1 { tr("expired_title") }
                    p("result error") {
                        attributes["role"] = "status"
                        tr("expired")
                    }
                } else {
                    h1 { tr("title") }
                    p { tr("intro", args["device"].orEmpty()) }
                    p { tr("code_label") }
                    p("result info") {
                        code("code") { +args["code"].orEmpty() }
                    }
                    p { tr("warning") }
                    div {
                        this.id = "login"
                        attributes["data-id"] = id
                        for (key in listOf("working", "done", "failed", "cancelled")) {
                            attributes["data-$key"] = t(key)
                        }
                        button(type = ButtonType.button, classes = "cta") {
                            this.id = "continue"
                            // Shown by the script once it knows the browser can use passkeys
                            attributes["hidden"] = ""
                            tr("button")
                        }
                        p("result error") {
                            this.id = "unsupported"
                            attributes["role"] = "status"
                            attributes["hidden"] = ""
                            tr("unsupported")
                        }
                        p("result") {
                            this.id = "status"
                            attributes["role"] = "status"
                            attributes["hidden"] = ""
                        }
                        noScript {
                            p("result error") { tr("js_required") }
                        }
                    }
                    script(src = "/static/browser-login.js") { defer = true }
                }
            }
        }
    }
}

/**
 * Sign-in with a passkey in the user's browser, for apps that can't use passkeys themselves (desktop):
 * 1. The app `POST`s `/auth/browser`, gets a [BrowserLoginRequest], opens its `url` in the browser, and listens to
 *    `/auth/browser/{id}/events` (SSE) with its secret.
 * 2. The page at `/browser-login` shows the request's code, for the user to check against the app's, and asks for
 *    their passkey. Its assertion is verified by `/auth/browser/{id}/complete`, which starts the session.
 * 3. The server tells the app through SSE, and the app redeems the tokens at `/auth/browser/{id}/redeem`: they never
 *    reach the browser, and only the holder of the secret gets them.
 */
fun Route.browserLoginRoutes() {
    get("/browser-login") {
        val pending = call.request.queryParameters["id"]?.let(BrowserLogins::findPending)
        val html = BrowserLoginPage.render(
            call.request.locale(),
            if (pending == null) emptyMap() else mapOf(
                "id" to pending.id,
                "code" to pending.code,
                "device" to pending.deviceName,
            ),
        )
        call.respondText(html, ContentType.Text.Html, if (pending == null) HttpStatusCode.NotFound else HttpStatusCode.OK)
    }

    rateLimit(RateLimits.AUTHENTICATION) {
        post<Api.Auth.Browser> {
            val request = call.receive<BrowserLoginStartRequest?>()
                ?: return@post respondError(Error.MissingArgument("device_name"))
            val deviceName = request.deviceName.trim().take(64).ifBlank { "Desktop" }

            val (pending, secret) = BrowserLogins.create(deviceName, ClientInfo.from(call))
            call.respond(
                BrowserLoginRequest(
                    id = pending.id,
                    secret = secret,
                    code = pending.code,
                    url = "${AppLinks.baseUrl}/browser-login?id=${pending.id}",
                    expiresIn = pending.remaining.inWholeSeconds,
                )
            )
        }

        post<Api.Auth.Browser.Complete> { resource ->
            val pending = BrowserLogins.findPending(resource.id)
                ?: return@post respondError(Error.EntityNotFound("BrowserLogin", resource.id))
            val request = call.receive<RestoreKeyVerificationRequest?>()
                ?: return@post respondError(Error.MissingArgument("authenticationResponseJson"))

            when (val result = verifyAssertion(request.authenticationResponseJson)) {
                is AssertionVerification.Failure ->
                    respondAuthError(AuthEventType.PASSKEY_LOGIN, null, result.error)
                is AssertionVerification.Success -> {
                    // Restore keys are created silently by apps: a browser only ever has passkeys.
                    if (result.kind != CredentialKind.PASSKEY) {
                        return@post respondAuthError(AuthEventType.PASSKEY_LOGIN, result.user.email, Error.IncorrectPasswordOrEmail())
                    }
                    // The session belongs to the app that asked, not to the browser that approved it.
                    val tokens = Database {
                        AuthTokens.startSession(result.user, AuthSessionMethod.PASSKEY, pending.client)
                    }
                    if (!BrowserLogins.approve(pending.id, tokens)) {
                        // Approved twice at once: the other one won.
                        Database { AuthTokens.revokeSessionOf(tokens.refreshToken, AuthSessionRevocationReason.LOGOUT) }
                        return@post respondError(Error.EntityNotFound("BrowserLogin", resource.id))
                    }
                    recordAuthEvent(AuthEventType.PASSKEY_LOGIN, result.user.email, null)
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }

        post<Api.Auth.Browser.Redeem> { resource ->
            val tokens = BrowserLogins.redeem(resource.id, call.request.headers[BROWSER_LOGIN_SECRET_HEADER])
                ?: return@post respondError(Error.EntityNotFound("BrowserLogin", resource.id))
            respondTokens(tokens)
        }
    }

    // Not a resource: Ktor has none for SSE.
    sse("/auth/browser/{id}/events") {
        val id = call.parameters["id"]
        val pending = id?.let { BrowserLogins.find(it, call.request.headers[BROWSER_LOGIN_SECRET_HEADER]) }
        if (pending == null) {
            logger.debug("Tried to listen to an unknown or expired browser login.")
            close()
            return@sse
        }

        send(event = "connection")
        if (BrowserLogins.awaitApproval(pending)) {
            send(data = "ok", event = BROWSER_LOGIN_APPROVED_EVENT)
        } else {
            send(data = "expired", event = BROWSER_LOGIN_EXPIRED_EVENT)
        }
    }
}
