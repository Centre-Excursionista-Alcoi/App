package org.centrexcursionistalcoi.app.routes

import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.json

/**
 * `/.well-known/apple-app-site-association` is what lets iOS open this server's links in the app and share its
 * saved passwords, the counterpart of `assetlinks.json` for Android.
 */
class TestWellKnownRoutes : ApplicationTestBase() {
    private val appId = "UQSVPP37UL.org.centrexcursionistalcoi.app"

    @AfterTest
    fun tearDown() {
        WellKnownConfigProvider.override(WellKnownConfigProvider.APPLE_APP_IDS_VARIABLE, null)
    }

    private fun useAppIds(appIds: String?) = WellKnownConfigProvider.override(WellKnownConfigProvider.APPLE_APP_IDS_VARIABLE, appIds)

    private suspend fun HttpResponse.association(): JsonObject = json.parseToJsonElement(bodyAsText()).jsonObject

    private val JsonObject.details: JsonArray get() = getValue("applinks").jsonObject.getValue("details").jsonArray
    private val JsonObject.credentialApps: List<String> get() = getValue("webcredentials").jsonObject.getValue("apps").jsonArray.map { it.jsonPrimitive.content }

    @Test
    fun test_aasa_isPublic_andServedAsJson() = runApplicationTest {
        // iOS fetches it anonymously, with no redirect and no login
        useAppIds(appId)

        client.get("/.well-known/apple-app-site-association").apply {
            assertStatusCode(HttpStatusCode.OK)
            assertTrue(headers[HttpHeaders.ContentType]!!.startsWith("application/json"), "content type was ${headers[HttpHeaders.ContentType]}")
        }
    }

    @Test
    fun test_aasa_listsTheConfiguredApp_forLinksAndPasswords() = runApplicationTest {
        useAppIds(appId)

        val association = client.get("/.well-known/apple-app-site-association").association()

        val detail = association.details.single().jsonObject
        assertEquals(listOf(appId), detail.getValue("appIDs").jsonArray.map { it.jsonPrimitive.content })
        assertEquals(listOf(appId), association.credentialApps)
    }

    @Test
    fun test_aasa_claimsEveryLink_exceptTheWebOnlyPages_whichComeFirst() = runApplicationTest {
        useAppIds(appId)

        val components = client.get("/.well-known/apple-app-site-association").association()
            .details.single().jsonObject.getValue("components").jsonArray.map { it.jsonObject }

        // The first match wins: the password reset page has to be excluded before "everything" claims it. Android
        // makes the same exception (MainActivity.WEB_ONLY_PATHS), since the page has no screen in the app.
        assertEquals(2, components.size)
        assertEquals("/reset_password", components[0].getValue("/").jsonPrimitive.content)
        assertTrue(components[0].getValue("exclude").jsonPrimitive.boolean)
        assertEquals("*", components[1].getValue("/").jsonPrimitive.content)
        assertTrue("exclude" !in components[1])
    }

    @Test
    fun test_aasa_acceptsSeveralApps_ignoringSpacesAndEmptyEntries() = runApplicationTest {
        useAppIds(" $appId , ,TEAMID.other.app,")

        val association = client.get("/.well-known/apple-app-site-association").association()

        val expected = listOf(appId, "TEAMID.other.app")
        assertEquals(expected, association.details.single().jsonObject.getValue("appIDs").jsonArray.map { it.jsonPrimitive.content })
        assertEquals(expected, association.credentialApps)
    }

    @Test
    fun test_aasa_withNoAppConfigured_claimsNothing() = runApplicationTest {
        useAppIds(null)

        val response = client.get("/.well-known/apple-app-site-association")
        response.assertStatusCode(HttpStatusCode.OK)
        val association = response.association()

        // valid, but naming no app: iOS opens every link in the browser
        assertEquals(0, association.details.size)
        assertEquals(emptyList(), association.credentialApps)
    }
}
