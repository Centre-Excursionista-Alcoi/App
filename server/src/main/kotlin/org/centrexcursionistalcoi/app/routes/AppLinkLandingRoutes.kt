package org.centrexcursionistalcoi.app.routes

import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.server.request.uri
import io.ktor.server.request.userAgent
import io.ktor.server.response.cacheControl
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import org.centrexcursionistalcoi.app.applink.AppLinkRoutes
import org.centrexcursionistalcoi.app.routes.WebTemplate.Companion.respondTemplate

private val appIconBytes: ByteArray by lazy {
    object {}.javaClass.getResourceAsStream("/web/app-icon.png")!!.readBytes()
}

/**
 * The "already have the app? open it -- otherwise get it" page (see [WebTemplate.GetTheApp]), served at every
 * [AppLinkRoutes.APP_ONLY] path -- both bare and with a trailing id, e.g. `/admin/lendings` and
 * `/admin/lendings/<id>`. Those paths only mean something inside the app (see `Destination.fromUrl`), so a
 * verified device already opens the app straight from the link and never makes this request at all: whoever does
 * reach this route has no app installed, or is in a context (an in-app browser, say) that skipped the OS's own
 * interception. The id, if any, isn't otherwise used: this page can't act on it without the app.
 */
fun Route.appLinkLandingRoutes() {
    suspend fun RoutingContext.respondGetTheApp() {
        call.respondTemplate(
            WebTemplate.GetTheApp,
            mapOf(
                "userAgent" to call.request.userAgent(),
                "path" to call.request.uri,
            ),
        )
    }

    for (path in AppLinkRoutes.APP_ONLY) {
        get("/$path") { respondGetTheApp() }
        get("/$path/{id}") { respondGetTheApp() }
    }

    get("/web/app-icon.png") {
        call.response.cacheControl(CacheControl.MaxAge(maxAgeSeconds = 60 * 60 * 24 * 7, visibility = CacheControl.Visibility.Public))
        call.respondBytes(appIconBytes, ContentType.Image.PNG)
    }
}
