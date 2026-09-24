package org.centrexcursionistalcoi.app.routes

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.centrexcursionistalcoi.app.AppLinks
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertStatusCode

private const val ANDROID_UA = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36"
private const val IOS_UA = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 Version/17.5 Mobile/15E148 Safari/604.1"
private const val DESKTOP_UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/120 Safari/537.36"

/**
 * What happens on [AppLinks.baseUrl]'s host (`centrexcursionistalcoi.app` by default): whoever reaches the server
 * there at all has no app installed, or is in a context (an in-app browser, say) that skipped the OS's own App
 * Link/Universal Link interception -- a device that has the app never makes this request. Every path behaves the
 * same, the bare domain included: iOS is sent straight to the store (a plain HTTPS redirect always works); every
 * other visitor -- Android included -- lands on the same landing page (see [WebTemplate.GetApp]), which links to
 * both stores and the regular website, and additionally carries a meta-refresh for Android that attempts to open
 * the app itself first. Android used to get a raw HTTP redirect straight into that attempt with no page behind
 * it, which left a blank page whenever the browser couldn't or wouldn't act on it (#689).
 *
 * Redirect tests use a client with `followRedirects = false`: `testApplication`'s default client follows a
 * `Location` by resubmitting it into the very same in-process test app, which fails outright for one that points
 * outside it -- exactly every case here (Apple, or a fake `intent://` embedded in the Android page).
 */
class TestAppLinkFallbackRoutes : ApplicationTestBase() {
    private val lendingId = UUID.fromString("1f0e5c2a-0000-4000-8000-000000000001")

    private suspend fun HttpClient.onAppLinksHost(path: String, userAgent: String? = null): HttpResponse = get(path) {
        header(HttpHeaders.Host, "centrexcursionistalcoi.app")
        userAgent?.let { header(HttpHeaders.UserAgent, it) }
    }

    private val everyAppLinksPath = listOf("/", "/admin/lendings", "/admin/lendings/$lendingId", "/admin/items/$lendingId", "/itemType/$lendingId", "/some/made/up/path")

    /** Pulls the URL out of `<meta http-equiv="refresh" content="0;url=...">`, unescaping the HTML entities it was escaped as. */
    private fun metaRefreshUrl(body: String): String {
        val match = Regex("""<meta http-equiv="refresh" content="0;url=([^"]*)">""").find(body)
            ?: error("No meta-refresh tag found in: $body")
        return match.groupValues[1]
            .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&#39;", "'")
    }

    @Test
    fun test_android_getsTheLandingPage_withAMetaRefreshToTheIntentLaunchUrl_onEveryPath() = runApplicationTest {
        val client = createClient { followRedirects = false }
        for (path in everyAppLinksPath) {
            val response = client.onAppLinksHost(path, ANDROID_UA)
            // Unlike the old raw-redirect behavior, this is always a real page -- so a browser that can't act on
            // the meta-refresh below still has something to show instead of a blank page (#689).
            response.assertStatusCode(HttpStatusCode.OK)
            assertTrue(response.contentType()?.match(ContentType.Text.Html) == true, "$path: ${response.contentType()}")

            val body = response.bodyAsText()
            val location = metaRefreshUrl(body)

            assertTrue(location.startsWith("intent://centrexcursionistalcoi.app$path"), "$path -> $location")
            assertContains(location, "scheme=https;")
            assertContains(location, "package=org.centrexcursionistalcoi.app;")
            assertContains(location, "S.browser_fallback_url=")

            // Same landing page as everyone else, including the website link (#689).
            assertContains(body, "https://play.google.com/store/apps/details?id=org.centrexcursionistalcoi.app")
            assertContains(body, AppLinks.appStoreUrl)
            assertContains(body, "https://centrexcursionistalcoi.org")
        }
    }

    // request.uri (attacker-controlled) flows straight into the meta-refresh's `content` attribute. `"`/`<`/`>`
    // can't reach here unencoded -- they're not valid raw in a URI path, and Ktor's path()/uri never decode
    // percent-encoding -- but `&` (a URI sub-delim) legitimately can, and a raw `&` inside an HTML attribute is
    // the start of a malformed/ambiguous entity reference, so ResourceWebTemplate now escapes every plain
    // `{{key}}` substitution by default (see WebTemplate.kt) rather than relying on what happens to be
    // unreachable today. This checks that escaping actually applies end to end, not just that it's unexploitable.
    @Test
    fun test_android_anAmpersandInThePath_isEscapedInTheMetaTag() = runApplicationTest {
        val client = createClient { followRedirects = false }
        val response = client.get("/foo&bar") {
            header(HttpHeaders.Host, "centrexcursionistalcoi.app")
            header(HttpHeaders.UserAgent, ANDROID_UA)
        }
        response.assertStatusCode(HttpStatusCode.OK)
        val body = response.bodyAsText()

        assertContains(body, "/foo&amp;bar")
        assertTrue("content=\"0;url=intent://centrexcursionistalcoi.app/foo&bar#" !in body, body)
    }

    @Test
    fun test_ios_isRedirectedStraightToTheAppStore_onEveryPath() = runApplicationTest {
        val client = createClient { followRedirects = false }
        for (path in everyAppLinksPath) {
            val response = client.onAppLinksHost(path, IOS_UA)
            response.assertStatusCode(HttpStatusCode.Found)
            assertEquals(AppLinks.appStoreUrl, response.headers[HttpHeaders.Location])
        }
    }

    @Test
    fun test_desktopOrUnknown_getsTheLandingPage_onEveryPath() = runApplicationTest {
        for ((path, userAgent) in listOf("/" to DESKTOP_UA, "/admin/lendings/$lendingId" to null)) {
            val response = client.onAppLinksHost(path, userAgent)
            response.assertStatusCode(HttpStatusCode.OK)
            assertTrue(response.contentType()?.match(ContentType.Text.Html) == true, "$path: ${response.contentType()}")

            val body = response.bodyAsText()
            assertContains(body, "https://play.google.com/store/apps/details?id=org.centrexcursionistalcoi.app")
            assertContains(body, AppLinks.appStoreUrl)
            assertContains(body, "/static/app-icon.png")
            assertContains(body, "/static/app-links.css")
            // Neither desktop nor a crawler should carry Android's app-open attempt (#689).
            assertTrue("http-equiv=\"refresh\"" !in body, body)
        }
    }

    // #689: the "get the app" popup should also let a visitor continue to the regular public website, on every
    // path and regardless of platform -- not just desktop's.
    @Test
    fun test_everyLandingPage_linksToTheRegularWebsite() = runApplicationTest {
        for (userAgent in listOf(DESKTOP_UA, ANDROID_UA, null)) {
            val body = client.onAppLinksHost("/", userAgent).bodyAsText()
            assertContains(body, "https://centrexcursionistalcoi.org", message = "userAgent=$userAgent")
        }
    }

    // ---- Link previews: what WhatsApp/Telegram/Slack/... show before anyone opens the link. They identify with
    // their own User-Agent (never "Android" or "iPhone"), so they land in the same branch a desktop browser does,
    // and only ever read the <head>. Deliberately generic -- see AppLinkFallbackRoutes.previewFor. ----

    private suspend fun HttpResponse.metaTag(property: String): String {
        val body = bodyAsText()
        return Regex("""(?:property|name)="$property" content="([^"]*)"""").find(body)?.groupValues?.get(1)
            ?: error("No meta tag for $property in: $body")
    }

    @Test
    fun test_preview_ofALending_isGeneric_namesNoOneAndNothingBorrowed() = runApplicationTest {
        val response = client.onAppLinksHost("/admin/lendings/$lendingId", "WhatsApp/2.24.1.78 A")
        response.assertStatusCode(HttpStatusCode.OK)
        val body = response.bodyAsText()

        assertEquals("Lending", response.metaTag("og:title"))
        assertTrue(response.metaTag("og:description").isNotBlank())
        // the only place the id may appear is the URL -- never as, or next to, a person's or an item's name
        assertTrue(lendingId.toString() !in response.metaTag("og:title"))
        assertTrue(lendingId.toString() !in response.metaTag("og:description"))
        assertContains(body, """<title>Lending</title>""")
    }

    @Test
    fun test_preview_ofAnAdminItem_isGeneric() = runApplicationTest {
        val response = client.onAppLinksHost("/admin/items/$lendingId")
        assertEquals("Inventory item", response.metaTag("og:title"))
    }

    @Test
    fun test_preview_ofAnItemType_isGeneric() = runApplicationTest {
        val response = client.onAppLinksHost("/itemType/$lendingId")
        assertEquals("Item", response.metaTag("og:title"))
    }

    @Test
    fun test_preview_ofAnUnrecognizedPath_fallsBackToTheAppItself() = runApplicationTest {
        for (path in listOf("/", "/some/made/up/path")) {
            assertEquals("CEA App", client.onAppLinksHost(path).metaTag("og:title"), path)
        }
    }

    @Test
    fun test_preview_image_isTheAppIconAsAnAbsoluteUrl() = runApplicationTest {
        val response = client.onAppLinksHost("/admin/lendings/$lendingId")
        assertEquals("https://centrexcursionistalcoi.app/static/app-icon.png", response.metaTag("og:image"))
        assertEquals(response.metaTag("og:image"), response.metaTag("twitter:image"))
    }

    @Test
    fun test_preview_url_isTheRequestsOwnPath_withNoQueryString() = runApplicationTest {
        val response = client.get("/admin/lendings/$lendingId?utm_source=email") {
            header(HttpHeaders.Host, "centrexcursionistalcoi.app")
        }
        assertEquals("https://centrexcursionistalcoi.app/admin/lendings/$lendingId", response.metaTag("og:url"))
    }

    // preview_url embeds the raw request path, which is attacker-controlled -- ResourceWebTemplate now escapes
    // every plain `{{key}}` substitution by default (see WebTemplate.kt) rather than trusting each call site to
    // remember to. metaTag() doesn't decode entities, so an escaped `&` shows up here as the literal `&amp;`.
    @Test
    fun test_preview_url_withAnAmpersandInThePath_isEscaped() = runApplicationTest {
        val response = client.get("/foo&bar") {
            header(HttpHeaders.Host, "centrexcursionistalcoi.app")
        }
        assertEquals("https://centrexcursionistalcoi.app/foo&amp;bar", response.metaTag("og:url"))
    }

    @Test
    fun test_preview_andTwitterCardAgree() = runApplicationTest {
        val response = client.onAppLinksHost("/admin/lendings/$lendingId")
        assertEquals("summary", response.metaTag("twitter:card"))
        assertEquals(response.metaTag("og:title"), response.metaTag("twitter:title"))
        assertEquals(response.metaTag("og:description"), response.metaTag("twitter:description"))
    }

    @Test
    fun test_notAppLinksHost_root_keepsAnnouncingTheApi() = runApplicationTest {
        // no Host override: this is the API's own behavior, unrelated to app links
        client.get("/").apply {
            assertStatusCode(HttpStatusCode.OK)
            assertEquals("Hello! The Centre Excursionista d'Alcoi API is running.", bodyAsText())
        }
    }

    @Test
    fun test_notAppLinksHost_unmatchedPath_getsAPlain404_notTheAppFallback() = runApplicationTest {
        val response = client.get("/totally-made-up-path")
        response.assertStatusCode(HttpStatusCode.NotFound)
        assertTrue("play.google.com" !in response.bodyAsText())
    }

    @Test
    fun test_aRouteThatMatchesButAnswers404ForItsOwnReasons_isNotHijacked() = runApplicationTest {
        // /events/{id} is a real, matched route; a missing entity's 404 is its own business logic, not "unmatched"
        client.get("/events/${UUID.randomUUID()}").apply {
            assertStatusCode(HttpStatusCode.NotFound)
            assertContains(bodyAsText(), "EntityNotFound")
        }
    }

    @Test
    fun test_staticAssets_areServed_regardlessOfHost() = runApplicationTest {
        client.get("/static/app-icon.png").apply {
            assertStatusCode(HttpStatusCode.OK)
            assertTrue(contentType()?.match(ContentType.Image.PNG) == true)
        }
        client.get("/static/app-links.css").apply {
            assertStatusCode(HttpStatusCode.OK)
            assertTrue(contentType()?.match(ContentType.Text.CSS) == true)
        }
    }

    @Test
    fun test_staticAssets_anUnknownFile_isAPlain404_notTheAppFallback() = runApplicationTest {
        // even on the app-links host: a missing static file is a 404, not the "get the app" page
        client.onAppLinksHost("/static/does-not-exist.png").assertStatusCode(HttpStatusCode.NotFound)
    }
}
