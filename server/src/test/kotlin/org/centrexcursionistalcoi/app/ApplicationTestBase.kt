package org.centrexcursionistalcoi.app

import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.setCookie
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.notifications.Push
import org.centrexcursionistalcoi.app.plugins.UserSession
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.security.AES
import org.centrexcursionistalcoi.app.test.*
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

abstract class ApplicationTestBase {

    fun runApplicationTest(
        shouldLogIn: LoginType = LoginType.NONE,
        mockDate: LocalDate? = null,
        mockNow: Instant? = null,
        /**
         * Patches to apply to the user entity after creation (only applies if [shouldLogIn] is [LoginType.USER] or [LoginType.ADMIN]).
         */
        userEntityPatches: JdbcTransaction.(UserReferenceEntity) -> Unit = {},
        disablePush: Boolean = true,
        block: suspend ApplicationTestBuilder.(ApplicationTestContext<Unit>) -> Unit
    ) = runApplicationTest(shouldLogIn, mockDate, mockNow, { }, userEntityPatches, disablePush) { block(it) }

    fun <DIB> runApplicationTest(
        shouldLogIn: LoginType = LoginType.NONE,
        mockDate: LocalDate? = null,
        mockNow: Instant? = null,
        databaseInitBlock: (JdbcTransaction.() -> DIB)? = null,
        /**
         * Patches to apply to the user entity after creation (only applies if [shouldLogIn] is [LoginType.USER] or [LoginType.ADMIN]).
         */
        userEntityPatches: JdbcTransaction.(UserReferenceEntity) -> Unit = {},
        disablePush: Boolean = true,
        finally: suspend () -> Unit = {},
        block: suspend ApplicationTestBuilder.(ApplicationTestContext<DIB>) -> Unit
    ) = runTest {
        mockDate?.let(::mockTime)
        mockNow?.let(::mockTime)

        Database.initForTests()

        AES.secretKey = AES.generateKey()

        // Disable push notifications during tests
        Push.disable = disablePush

        try {
            val dib = databaseInitBlock?.let { Database(it) }

            if (shouldLogIn == LoginType.USER) Database { FakeUser.provideEntity().also { userEntityPatches(it) } }
            else if (shouldLogIn == LoginType.ADMIN) Database { FakeAdminUser.provideEntity().also { userEntityPatches(it) } }

            testApplication {
                application {
                    module(isTesting = true)

                    routing {
                        get("/test-login") {
                            // Simulate a user
                            val fakeUser = UserSession(
                                sub = FakeUser.SUB,
                                fullName = FakeUser.FULL_NAME,
                                email = FakeUser.EMAIL,
                                groups = FakeUser.GROUPS
                            )

                            call.sessions.set(fakeUser)
                            getUserSessionOrFail()

                            call.respondText("Logged in as ${fakeUser.fullName}")
                        }
                        get("/test-login-admin") {
                            // Simulate a user
                            val fakeUser = UserSession(
                                sub = FakeAdminUser.SUB,
                                fullName = FakeAdminUser.FULL_NAME,
                                email = FakeAdminUser.EMAIL,
                                groups = FakeAdminUser.GROUPS
                            )

                            call.sessions.set(fakeUser)
                            getUserSessionOrFail()

                            call.respondText("Logged in as ${fakeUser.fullName}")
                        }
                    }
                }
                val cookiesStorage = AcceptAllCookiesStorage()
                client = createClient {
                    install(ContentNegotiation) {
                        json(json)
                    }
                    install(HttpCookies) {
                        storage = cookiesStorage
                    }
                    install(Logging) {
                        level = LogLevel.ALL
                    }
                    install(SSE)
                }

                if (shouldLogIn == LoginType.USER) loginAsFakeUser()
                else if (shouldLogIn == LoginType.ADMIN) loginAsFakeAdminUser()

                val context = ApplicationTestContext(dib, cookiesStorage)
                block(context)
            }
        } finally {
            // Re-enable push for tests that require it
            Push.disable = false

            Database.clear()
            finally()

            resetTimeFunctions()
        }
    }

    suspend fun ApplicationTestBuilder.loginAsFakeUser() {
        val response = client.get("/test-login")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("true", response.headers["CEA-LoggedIn"])
        assertNotNull(
            response.setCookie().find { it.name == UserSession.COOKIE_NAME },
            "Session cookie not found in response"
        )
        System.err.println("Logged in successfully!")
    }

    suspend fun ApplicationTestBuilder.loginAsFakeAdminUser() {
        val response = client.get("/test-login-admin")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("true", response.headers["CEA-LoggedIn"])
        assertNotNull(
            response.setCookie().find { it.name == UserSession.COOKIE_NAME },
            "Session cookie not found in response"
        )
        System.err.println("Logged in successfully!")
    }
}
