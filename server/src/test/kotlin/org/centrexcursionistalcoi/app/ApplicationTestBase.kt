package org.centrexcursionistalcoi.app

import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.testing.*
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.notifications.Push
import org.centrexcursionistalcoi.app.plugins.UserSession
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.security.AES
import org.centrexcursionistalcoi.app.test.FakeAdminUser
import org.centrexcursionistalcoi.app.test.FakeLendingUser
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.LoginType
import org.centrexcursionistalcoi.app.test.StubUser
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import java.time.Instant
import java.time.LocalDate
import javax.crypto.spec.IvParameterSpec
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

abstract class ApplicationTestBase {

    fun runApplicationTest(
        shouldLogIn: LoginType = LoginType.NONE,
        mockDate: LocalDate? = null,
        mockNow: Instant? = null,
        /**
         * Patches to apply to the user entity after creation (only applies if [shouldLogIn] is [LoginType.USER], [LoginType.ADMIN], [LoginType.LENDING_USER], or [LoginType.CUSTOM]).
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
         * Patches to apply to the user entity after creation (only applies if [shouldLogIn] is [LoginType.USER], [LoginType.ADMIN], [LoginType.LENDING_USER], or [LoginType.CUSTOM]).
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
        AES.ivParameterSpec = IvParameterSpec(ByteArray(16) { 0 }) // Example IV

        // Disable push notifications during tests
        Push.disable = disablePush

        try {
            val dib = databaseInitBlock?.let { Database(it) }

            when (shouldLogIn) {
                is LoginType.USER -> Database { FakeUser.provideEntity().also { userEntityPatches(it) } }
                is LoginType.ADMIN -> Database { FakeAdminUser.provideEntity().also { userEntityPatches(it) } }
                is LoginType.LENDING_USER -> Database { FakeLendingUser.provideEntity().also { userEntityPatches(it) } }
                is LoginType.CUSTOM -> Database { shouldLogIn.user.provideEntity().also { userEntityPatches(it) } }
                is LoginType.NONE -> {}
            }

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
                        get("/test-login-lending") {
                            // Simulate a lending user
                            val fakeUser = UserSession(
                                sub = FakeLendingUser.SUB,
                                fullName = FakeLendingUser.FULL_NAME,
                                email = FakeLendingUser.EMAIL,
                                groups = FakeLendingUser.GROUPS
                            )

                            call.sessions.set(fakeUser)
                            getUserSessionOrFail()

                            call.respondText("Logged in as ${fakeUser.fullName}")
                        }
                        get("/test-login-custom") {
                            // For custom users, we need to pass the user info through request
                            // This will be handled by the loginAsCustomUser function
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

                when (shouldLogIn) {
                    is LoginType.USER -> loginAsFakeUser()
                    is LoginType.ADMIN -> loginAsFakeAdminUser()
                    is LoginType.LENDING_USER -> loginAsFakeLendingUser()
                    is LoginType.CUSTOM -> loginAsCustomUser(shouldLogIn.user)
                    is LoginType.NONE -> {}
                }

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

    suspend fun ApplicationTestBuilder.loginAsFakeLendingUser() {
        val response = client.get("/test-login-lending")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("true", response.headers["CEA-LoggedIn"])
        assertNotNull(
            response.setCookie().find { it.name == UserSession.COOKIE_NAME },
            "Session cookie not found in response"
        )
        System.err.println("Logged in successfully!")
    }

    suspend fun ApplicationTestBuilder.loginAsCustomUser(user: StubUser) {
        // For custom users, we need to add a dynamic route handler
        // For now, we'll use a simpler approach with direct session setting
        throw UnsupportedOperationException("Custom user login is not yet fully implemented. Use predefined user types.")
    }
}
