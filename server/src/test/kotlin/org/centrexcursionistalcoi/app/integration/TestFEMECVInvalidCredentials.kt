package org.centrexcursionistalcoi.app.integration

import app.cash.turbine.test
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.resources.delete
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.ResourcesUtils
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.integration.femecv.FEMECVException
import org.centrexcursionistalcoi.app.integration.femecv.FEMECVInvalidCredentialsException
import org.centrexcursionistalcoi.app.notifications.Push
import org.centrexcursionistalcoi.app.now
import org.centrexcursionistalcoi.app.push.PushNotification
import org.centrexcursionistalcoi.app.request.LinkFEMECVRequest
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.routes.Api
import org.centrexcursionistalcoi.app.security.UserSession
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.LoginType
import org.centrexcursionistalcoi.app.json

/** What happens when FEMECV rejects the credentials a user saved (#813). */
class TestFEMECVInvalidCredentials : ApplicationTestBase() {
    @AfterTest
    fun resetEngine() = FEMECV.resetEngine()

    private val loginRequests = AtomicInteger()

    private val wrongPassword = "<div class=\"errorBox\">El usuario o la contraseña no son correctos, vuélvalo a intentar por favor.</div>"

    private fun loginAnswers(body: String) {
        FEMECV.engine = MockEngine { request ->
            if (request.url.encodedPath == "/FormLogin.php") {
                loginRequests.incrementAndGet()
                respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, ContentType.Text.Html.toString()))
            } else {
                respond("Not Found", HttpStatusCode.NotFound)
            }
        }
    }

    /** FEMECV accepts any login, and has one license. */
    private fun femecvWorks() {
        FEMECV.engine = MockEngine { request ->
            val html = ContentType.Text.Html.toString()
            when (request.url.encodedPath) {
                "/FormLogin.php" -> {
                    loginRequests.incrementAndGet()
                    respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "/PanellControlUsuariFederat.php"))
                }
                "/PanellControlUsuariFederat.php" -> respond(ResourcesUtils.bytesFromResource("/femecv/licencias.html"), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, html))
                "/FormLlicencia.php" -> respond(ResourcesUtils.bytesFromResource("/femecv/licencia.html"), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, html))
                "/print/printAutoritzacioLlicencia.php" -> respond(ByteArray(16) { it.toByte() }, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, ContentType.Application.Pdf.toString()))
                else -> respond("Not Found", HttpStatusCode.NotFound)
            }
        }
    }

    private val userWithCredentials: org.jetbrains.exposed.v1.jdbc.JdbcTransaction.() -> UserReferenceEntity = {
        FakeUser.provideEntity().apply {
            femecvUsername = "user"
            femecvPassword = "password"
        }
    }

    private fun reference() = Database { UserReferenceEntity[FakeUser.SUB] }

    private val userSession get() = UserSession(sub = FakeUser.SUB, fullName = "User", email = "user@example.com", groups = emptyList())

    @Test
    fun `only an error about the username or the password is told apart`() = runTest {
        loginAnswers(wrongPassword)
        assertFailsWith<FEMECVInvalidCredentialsException> { FEMECV.login("user", "wrong") }

        loginAnswers("<div class=\"errorBox\">Usuari o contrasenya incorrectes</div>")
        assertFailsWith<FEMECVInvalidCredentialsException> { FEMECV.login("user", "wrong") }

        // FEMECV having trouble, or changing its page, is not the user's fault
        loginAnswers("<div class=\"errorBox\">Servicio no disponible por mantenimiento</div>")
        assertFailsWith<FEMECVException> { FEMECV.login("user", "right") }.also { assertFalse(it is FEMECVInvalidCredentialsException) }

        loginAnswers("<html><body>Something else</body></html>")
        assertFailsWith<FEMECVException> { FEMECV.login("user", "right") }.also { assertFalse(it is FEMECVInvalidCredentialsException) }
    }

    @Test
    fun `wrong credentials are saved, shown in the profile and not tried again`() = runApplicationTest(
        shouldLogIn = LoginType.USER, databaseInitBlock = userWithCredentials,
    ) {
        loginAnswers(wrongPassword)

        val first = client.get(Api.Profile())
        first.assertStatusCode(HttpStatusCode.OK)
        assertTrue(first.body<ProfileResponse>().femecvCredentialsInvalid)
        assertNotNull(Database { reference().femecvCredentialsInvalidSince })
        assertEquals(1, loginRequests.get())

        // Whatever else happens, FEMECV isn't asked again with the same password
        FEMECV.syncUsers()
        assertTrue(client.get(Api.Profile()).body<ProfileResponse>().femecvCredentialsInvalid)
        assertEquals(1, loginRequests.get())
    }

    @Test
    fun `the user is reminded once every 7 days`() = runApplicationTest(
        shouldLogIn = LoginType.USER, databaseInitBlock = userWithCredentials,
    ) {
        loginAnswers(wrongPassword)

        Push.flow(userSession).test {
            // The first failure tells them
            assertFailsWith<FEMECVInvalidCredentialsException> { reference().refreshFEMECVData() }
            assertIs<PushNotification.FemecvCredentialsInvalid>(awaitItem())

            // The daily sync, the next days: nothing
            FEMECV.syncUsers()
            FEMECV.syncUsers()
            expectNoEvents()

            // A week later: again
            Database { reference().femecvCredentialsReminderAt = now() - 8.days }
            FEMECV.syncUsers()
            val reminder = awaitItem()
            assertIs<PushNotification.FemecvCredentialsInvalid>(reminder)
            assertEquals(FakeUser.SUB, reminder.userSub)
            FEMECV.syncUsers()
            expectNoEvents()
        }
        assertEquals(1, loginRequests.get())
    }

    @Test
    fun `entering the credentials again fixes it`() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            userWithCredentials().apply {
                femecvCredentialsInvalidSince = now() - 3.days
                femecvCredentialsReminderAt = now() - 3.days
            }
        },
    ) {
        femecvWorks()

        val response = client.post(Api.Profile.FEMECVSync()) {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(LinkFEMECVRequest.serializer(), LinkFEMECVRequest("user", "new-password")))
        }
        response.assertStatusCode(HttpStatusCode.OK)

        Database {
            assertNull(reference().femecvCredentialsInvalidSince)
            assertNull(reference().femecvCredentialsReminderAt)
            assertEquals("new-password", reference().femecvPassword)
        }
        assertFalse(client.get(Api.Profile()).body<ProfileResponse>().femecvCredentialsInvalid)
    }

    @Test
    fun `wrong new credentials are refused and the old ones stay flagged`() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { userWithCredentials().apply { femecvCredentialsInvalidSince = now() - 3.days } },
    ) {
        loginAnswers(wrongPassword)

        val response = client.post(Api.Profile.FEMECVSync()) {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(LinkFEMECVRequest.serializer(), LinkFEMECVRequest("user", "still-wrong")))
        }
        response.assertStatusCode(HttpStatusCode.Unauthorized)

        assertNotNull(Database { reference().femecvCredentialsInvalidSince })
        assertEquals("password", Database { reference().femecvPassword })
    }

    @Test
    fun `disconnecting forgets it`() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            userWithCredentials().apply {
                femecvCredentialsInvalidSince = now() - 3.days
                femecvCredentialsReminderAt = now() - 3.days
            }
        },
    ) {
        client.delete(Api.Profile.FEMECVSync()).assertStatusCode(HttpStatusCode.NoContent)

        Database {
            assertNull(reference().femecvCredentialsInvalidSince)
            assertNull(reference().femecvCredentialsReminderAt)
        }
        assertFalse(client.get(Api.Profile()).body<ProfileResponse>().femecvCredentialsInvalid)
    }

    @Test
    fun `a FEMECV outage is not taken as wrong credentials`() = runApplicationTest(
        shouldLogIn = LoginType.USER, databaseInitBlock = userWithCredentials,
    ) {
        loginAnswers("<html><body>Maintenance</body></html>")

        assertFailsWith<FEMECVException> { reference().refreshFEMECVData() }

        assertNull(Database { reference().femecvCredentialsInvalidSince })
        // Tried again the next day
        FEMECV.syncUsers()
        assertEquals(2, loginRequests.get())
    }
}
