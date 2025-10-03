package org.centrexcursionistalcoi.app

import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
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
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

abstract class ApplicationTestBase {

    object FakeUser {
        const val SUB = "test-user-id-123"
        const val USERNAME = "user"
        const val EMAIL = "user@example.com"
        val GROUPS = listOf("user")
    }

    object FakeAdminUser {
        const val SUB = "test-user-id-456"
        const val USERNAME = "admin"
        const val EMAIL = "admin@example.com"
        val GROUPS = listOf(ADMIN_GROUP_NAME, "user")
    }

    protected fun runApplicationTest(
        block: suspend ApplicationTestBuilder.(ApplicationTestContext<Unit>) -> Unit
    ) = runApplicationTest({ }) { block(it) }

    protected fun <DIB> runApplicationTest(
        databaseInitBlock: (JdbcTransaction.() -> DIB)? = null,
        block: suspend ApplicationTestBuilder.(ApplicationTestContext<DIB>) -> Unit
    ) = runTest {
        Database.init(TEST_URL)

        try {
            val dib = databaseInitBlock?.let { Database(it) }

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
                }

                val context = ApplicationTestContext(dib, cookiesStorage)
                block(context)
            }
        } finally {
            Database.clear()
        }
    }

    protected suspend fun ApplicationTestBuilder.loginAsFakeUser() {
        val response = client.get("/test-login")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("true", response.headers["CEA-LoggedIn"])
        assertNotNull(
            response.setCookie().find { it.name == UserSession.COOKIE_NAME },
            "Session cookie not found in response"
        )
        System.err.println("Logged in successfully!")
    }

    protected suspend fun ApplicationTestBuilder.loginAsFakeAdminUser() {
        val response = client.get("/test-login-admin")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("true", response.headers["CEA-LoggedIn"])
        assertNotNull(
            response.setCookie().find { it.name == UserSession.COOKIE_NAME },
            "Session cookie not found in response"
        )
        System.err.println("Logged in successfully!")
    }

    protected suspend fun HttpResponse.assertStatusCode(expected: HttpStatusCode) {
        assert(expected == status) {
            "Expected $expected response. Got: $status. Body: ${bodyAsText()}"
        }
    }

    protected suspend fun HttpResponse.assertBadRequest() {
        assertStatusCode(HttpStatusCode.BadRequest)
    }
}
