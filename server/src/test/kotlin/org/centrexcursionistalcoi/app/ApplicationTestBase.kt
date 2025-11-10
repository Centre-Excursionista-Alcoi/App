package org.centrexcursionistalcoi.app

import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
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
import javax.crypto.spec.IvParameterSpec
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.Database.TEST_URL
import org.centrexcursionistalcoi.app.plugins.UserSession
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.security.AES
import org.centrexcursionistalcoi.app.test.FakeAdminUser
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.LoginType
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

abstract class ApplicationTestBase {

    fun runApplicationTest(
        shouldLogIn: LoginType = LoginType.NONE,
        block: suspend ApplicationTestBuilder.(ApplicationTestContext<Unit>) -> Unit
    ) = runApplicationTest(shouldLogIn, { }) { block(it) }

    fun <DIB> runApplicationTest(
        shouldLogIn: LoginType = LoginType.NONE,
        databaseInitBlock: (JdbcTransaction.() -> DIB)? = null,
        finally: suspend () -> Unit = {},
        block: suspend ApplicationTestBuilder.(ApplicationTestContext<DIB>) -> Unit
    ) = runTest {
        Database.init(TEST_URL)

        AES.secretKey = AES.generateKey()
        AES.ivParameterSpec = IvParameterSpec(ByteArray(16) { 0 }) // Example IV

        try {
            val dib = databaseInitBlock?.let { Database(it) }

            if (shouldLogIn == LoginType.USER) Database { FakeUser.provideEntity() }
            else if (shouldLogIn == LoginType.ADMIN) Database { FakeAdminUser.provideEntity() }

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
                }

                if (shouldLogIn == LoginType.USER) loginAsFakeUser()
                else if (shouldLogIn == LoginType.ADMIN) loginAsFakeAdminUser()

                val context = ApplicationTestContext(dib, cookiesStorage)
                block(context)
            }
        } finally {
            Database.clear()
            finally()
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
