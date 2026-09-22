package org.centrexcursionistalcoi.app.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.host
import io.ktor.server.request.uri
import io.ktor.server.request.userAgent
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.net.URLEncoder
import org.centrexcursionistalcoi.app.AppLinks
import org.centrexcursionistalcoi.app.routes.WebTemplate.Companion.respondTemplate

/** [AppLinks.baseUrl]'s own host, with no scheme -- the only host the app claims links on. */
private val appLinksHost: String by lazy { AppLinks.baseUrl.substringAfter("://") }

/**
 * Whether this request is for the domain app links are on. The fallback below only applies there; any other
 * host (the API's own `server.` domain included) keeps its ordinary behavior.
 */
private fun ApplicationCall.isAppLinksHost() = request.host() == appLinksHost

/**
 * The response for a request that the app itself would have intercepted if it were installed: whoever reaches
 * this is on [AppLinks.baseUrl]'s host with a path that has no real content of its own (nothing else matched it,
 * or matched it on purpose, see [appLinkFallbackRoutes]) -- an app-link path (`/admin/lendings/<id>`), a stray
 * link, or the bare domain.
 *
 * On mobile, sent straight into opening the app (Android) or the store, with no intermediate page. Anything else
 * gets a brief page linking to both stores (see [WebTemplate.GetApp]).
 */
private suspend fun ApplicationCall.respondAppLinkFallback() {
    val userAgent = request.userAgent().orEmpty()
    when {
        userAgent.contains("Android", ignoreCase = true) -> {
            // Relaunches this same request through Chrome's "intent://" scheme: it opens the app directly if
            // it's installed, or follows the fallback straight to the Play Store if not -- one redirect, no page.
            val fallback = URLEncoder.encode(AppLinks.playStoreUrl, "UTF-8")
            val launchUrl = "intent://$appLinksHost${request.uri}#Intent;scheme=https;" +
                "package=${AppLinks.ANDROID_PACKAGE_NAME};S.browser_fallback_url=$fallback;end"
            respondRedirect(launchUrl, permanent = false)
        }
        Regex("iPhone|iPad|iPod", RegexOption.IGNORE_CASE).containsMatchIn(userAgent) -> {
            // iOS has no equivalent to intent:// (there's no custom URL scheme declared, on purpose): if this
            // request is even reaching the server, the OS's own Universal Link interception already didn't
            // happen, so there's nothing left to try except sending them to the store directly.
            respondRedirect(AppLinks.appStoreUrl, permanent = false)
        }
        else -> respondTemplate(WebTemplate.GetApp, emptyMap())
    }
}

/**
 * Runs [respondAppLinkFallback] if this request is for [AppLinks.baseUrl]'s host; otherwise runs [default]
 * unchanged. For a route that exists on every host but should only behave differently on that one (currently
 * just the bare `/`, see `configureRouting`).
 */
suspend fun ApplicationCall.respondAppLinkFallbackOr(default: suspend ApplicationCall.() -> Unit) {
    if (isAppLinksHost()) respondAppLinkFallback() else default()
}

/**
 * Everything an app link can need beyond the bare `/` (wired separately, see [respondAppLinkFallbackOr]):
 * - Every otherwise-unmatched path -- an app-link path included, since none of those correspond to real
 *   server-rendered content -- falls back the same way, on [AppLinks.baseUrl]'s host. Ktor always prefers a more
 *   specific match over this tailcard, including a route that itself goes on to respond 404 for its own reasons
 *   (e.g. `Error.EntityNotFound` for a real but missing entity), so this never touches those; any other host
 *   keeps its ordinary 404.
 * - Whatever static assets [WebTemplate.GetApp] (or anything else) references, from `resources/static/` --
 *   e.g. `/static/app-icon.png` serves `resources/static/app-icon.png`, with no per-file route to add or forget.
 */
fun Route.appLinkFallbackRoutes() {
    get("/{...}") {
        call.respondAppLinkFallbackOr { call.respond(HttpStatusCode.NotFound) }
    }

    staticResources("/static", "static")
}
