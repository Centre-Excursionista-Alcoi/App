package org.centrexcursionistalcoi.app.routes

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.centrexcursionistalcoi.app.AppLinks
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertStatusCode

private const val ANDROID_UA = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36"
private const val IOS_UA = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 Version/17.5 Mobile/15E148 Safari/604.1"
private const val DESKTOP_UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/120 Safari/537.36"

/**
 * `/admin/lendings`, `/admin/items` and `/itemType` (see `AppLinkRoutes.APP_ONLY`) don't correspond to real
 * server-rendered content -- only the app knows what to do with them. A device with the app installed never
 * reaches these routes at all (the OS intercepts the link first); this is only ever seen by whoever doesn't have
 * the app, or is in a context (an in-app browser, say) that skipped that interception.
 */
class TestAppLinkLandingRoutes : ApplicationTestBase() {
    private val lendingId = UUID.fromString("1f0e5c2a-0000-4000-8000-000000000001")

    @AfterTest
    fun tearDown() {
        AppLinks.override("APP_LINKS_APP_STORE_URL", null)
    }

    @Test
    fun test_everyAppOnlyPath_respondsWithTheLandingPage() = runApplicationTest {
        for (path in listOf("/admin/lendings", "/admin/lendings/$lendingId", "/admin/items/$lendingId", "/itemType/$lendingId")) {
            client.get(path).apply {
                assertStatusCode(HttpStatusCode.OK)
                assertTrue(contentType()?.match(ContentType.Text.Html) == true, "$path: expected HTML, got ${contentType()}")
            }
        }
    }

    @Test
    fun test_aPathOutsideAppLinkRoutes_isNotServedByThisRoute() = runApplicationTest {
        // sanity check that this isn't accidentally a catch-all
        client.get("/not-an-app-link").assertStatusCode(HttpStatusCode.NotFound)
    }

    @Test
    fun test_android_getsAnIntentLaunchButton_pointingAtThisExactPath() = runApplicationTest {
        val body = client.get("/admin/lendings/$lendingId") { header(HttpHeaders.UserAgent, ANDROID_UA) }.bodyAsText()

        assertContains(body, "intent://centrexcursionistalcoi.app/admin/lendings/$lendingId#Intent;")
        assertContains(body, "scheme=https;")
        assertContains(body, "package=org.centrexcursionistalcoi.app;")
        assertContains(body, "S.browser_fallback_url=")
        // the Play Store link, reachable without going through the intent:// redirect
        assertContains(body, "https://play.google.com/store/apps/details?id=org.centrexcursionistalcoi.app")
    }

    @Test
    fun test_ios_withNoAppStoreLinkConfigured_showsNoBrokenButton() = runApplicationTest {
        val body = client.get("/admin/lendings/$lendingId") { header(HttpHeaders.UserAgent, IOS_UA) }.bodyAsText()

        assertFalse(body.contains("class=\"cta\""), "no App Store link is configured, so there's nothing to link a button to")
        assertFalse(body.contains("apple-itunes-app"))
    }

    @Test
    fun test_ios_withAnAppStoreLinkConfigured_showsTheButtonAndTheSmartBanner() = runApplicationTest {
        AppLinks.override("APP_LINKS_APP_STORE_URL", "https://apps.apple.com/us/app/cea-app/id1234567890")

        val body = client.get("/admin/lendings/$lendingId") { header(HttpHeaders.UserAgent, IOS_UA) }.bodyAsText()

        assertContains(body, "href=\"https://apps.apple.com/us/app/cea-app/id1234567890\"")
        // Safari's own "you have this app" banner, driven by the numeric id at the end of the App Store link
        assertContains(body, "<meta name=\"apple-itunes-app\" content=\"app-id=1234567890\">")
        assertFalse(body.contains("intent://"), "the Android launch link should not show up for an iOS visitor")
    }

    @Test
    fun test_desktopOrUnknown_getsNoLaunchButton_justAnExplanation() = runApplicationTest {
        AppLinks.override("APP_LINKS_APP_STORE_URL", "https://apps.apple.com/us/app/cea-app/id1234567890")

        val body = client.get("/admin/lendings/$lendingId") { header(HttpHeaders.UserAgent, DESKTOP_UA) }.bodyAsText()

        assertFalse(body.contains("class=\"cta\""))
        assertFalse(body.contains("intent://"))
    }

    @Test
    fun test_noUserAgentAtAll_doesNotCrash() = runApplicationTest {
        client.get("/admin/lendings/$lendingId").assertStatusCode(HttpStatusCode.OK)
    }

    @Test
    fun test_appIcon_isServedAsAPngWithALongCacheLifetime() = runApplicationTest {
        client.get("/web/app-icon.png").apply {
            assertStatusCode(HttpStatusCode.OK)
            assertTrue(contentType()?.match(ContentType.Image.PNG) == true)
            val cacheControl = headers[HttpHeaders.CacheControl].orEmpty()
            assertContains(cacheControl, "public")
            assertContains(cacheControl, "max-age")
        }
    }
}
