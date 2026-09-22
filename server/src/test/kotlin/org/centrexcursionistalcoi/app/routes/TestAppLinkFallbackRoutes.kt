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
import kotlin.test.AfterTest
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
 * same, the bare domain included: no page is shown on mobile, just a redirect straight into opening the app
 * (Android) or the store; anything else gets a brief page linking to both stores.
 *
 * Redirect tests use a client with `followRedirects = false`: `testApplication`'s default client follows a
 * `Location` by resubmitting it into the very same in-process test app, which fails outright for one that points
 * outside it -- exactly every case here (Apple, Google Play, or a fake `intent://`).
 */
class TestAppLinkFallbackRoutes : ApplicationTestBase() {
    private val lendingId = UUID.fromString("1f0e5c2a-0000-4000-8000-000000000001")

    @AfterTest
    fun tearDown() {
        AppLinks.override("APP_LINKS_APP_STORE_URL", null)
    }

    private suspend fun HttpClient.onAppLinksHost(path: String, userAgent: String? = null): HttpResponse = get(path) {
        header(HttpHeaders.Host, "centrexcursionistalcoi.app")
        userAgent?.let { header(HttpHeaders.UserAgent, it) }
    }

    private val everyAppLinksPath = listOf("/", "/admin/lendings", "/admin/lendings/$lendingId", "/admin/items/$lendingId", "/itemType/$lendingId", "/some/made/up/path")

    @Test
    fun test_android_isRedirectedToTheIntentLaunchUrl_onEveryPath() = runApplicationTest {
        val client = createClient { followRedirects = false }
        for (path in everyAppLinksPath) {
            val response = client.onAppLinksHost(path, ANDROID_UA)
            response.assertStatusCode(HttpStatusCode.Found)
            val location = response.headers[HttpHeaders.Location]!!

            assertTrue(location.startsWith("intent://centrexcursionistalcoi.app$path"), "$path -> $location")
            assertContains(location, "scheme=https;")
            assertContains(location, "package=org.centrexcursionistalcoi.app;")
            assertContains(location, "S.browser_fallback_url=")
        }
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
    fun test_ios_usesTheConfiguredAppStoreUrl() = runApplicationTest {
        AppLinks.override("APP_LINKS_APP_STORE_URL", "https://apps.apple.com/us/app/cea-app/id999")
        val client = createClient { followRedirects = false }

        val response = client.onAppLinksHost("/", IOS_UA)

        assertEquals("https://apps.apple.com/us/app/cea-app/id999", response.headers[HttpHeaders.Location])
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
        }
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
