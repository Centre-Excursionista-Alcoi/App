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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.Database.TEST_URL
import org.centrexcursionistalcoi.app.plugins.UserSession
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.security.OIDCConfig
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

        OIDCConfig.override("OAUTH_AUTHENTIK_BASE", "https://auth.example.com/")
        OIDCConfig.override("OAUTH_AUTHENTIK_TOKEN", "test-token")

        OIDCConfig.override("OAUTH_CLIENT_ID", "ZvPaQu8nsU1fpaSkt3c4MPDFKue2RrpGrEdEbiTU")
        OIDCConfig.override("OAUTH_CLIENT_SECRET", "pcG88eMDxemVywVlLeDrbJEzWIYuGNUFjf0jf85d")
        OIDCConfig.override("OAUTH_ISSUER", "https://auth.example.com/application/o/cea-app/")
        OIDCConfig.override("OAUTH_AUTH_ENDPOINT", "https://auth.example.com/application/o/authorize/")
        OIDCConfig.override("OAUTH_TOKEN_ENDPOINT", "https://auth.example.com/application/o/token/")
        OIDCConfig.override("OAUTH_USERINFO_ENDPOINT", "https://auth.example.com/application/o/userinfo/")
        OIDCConfig.override("OAUTH_JWKS_ENDPOINT", "https://auth.example.com/application/o/cea-app/jwks/")
        OIDCConfig.override("OAUTH_REDIRECT_URI", "http://localhost:8080/callback")

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
                                username = FakeUser.USERNAME,
                                email = FakeUser.EMAIL,
                                groups = FakeUser.GROUPS
                            )

                            call.sessions.set(fakeUser)
                            getUserSessionOrFail()

                            call.respondText("Logged in as ${fakeUser.username}")
                        }
                        get("/test-login-admin") {
                            // Simulate a user
                            val fakeUser = UserSession(
                                sub = FakeAdminUser.SUB,
                                username = FakeAdminUser.USERNAME,
                                email = FakeAdminUser.EMAIL,
                                groups = FakeAdminUser.GROUPS
                            )

                            call.sessions.set(fakeUser)
                            getUserSessionOrFail()

                            call.respondText("Logged in as ${fakeUser.username}")
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
