package org.centrexcursionistalcoi.app

import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.sse.SSE
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.table.AuthSessionMethod
import org.centrexcursionistalcoi.app.notifications.Email
import org.centrexcursionistalcoi.app.notifications.Push
import org.centrexcursionistalcoi.app.security.AES
import org.centrexcursionistalcoi.app.security.AuthTokens
import org.centrexcursionistalcoi.app.security.ClientInfo
import org.centrexcursionistalcoi.app.storage.RedisStoreMap
import org.centrexcursionistalcoi.app.test.FakeAdminUser
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.FakeUser2
import org.centrexcursionistalcoi.app.test.StubUser
import org.centrexcursionistalcoi.app.test.LoginType
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
        disableEmail: Boolean = true,
        finally: suspend () -> Unit = {},
        block: suspend ApplicationTestBuilder.(ApplicationTestContext<DIB>) -> Unit
    ) = runTest {
        mockDate?.let(::mockTime)
        mockNow?.let(::mockTime)

        Database.initForTests()

        AES.secretKey = AES.generateKey()

        // Disable push notifications during tests
        Push.disable = disablePush

        // Disable email during tests
        Email.disabled = disableEmail

        try {
            val dib = databaseInitBlock?.let { Database(it) }

            if (shouldLogIn == LoginType.USER) Database { FakeUser.provideEntity().also { userEntityPatches(it) } }
            else if (shouldLogIn == LoginType.ADMIN) Database { FakeAdminUser.provideEntity().also { userEntityPatches(it) } }

            testApplication {
                application {
                    module(isTesting = true)
                }
                client = createClient {
                    install(ContentNegotiation) {
                        json(json)
                    }
                    install(Logging) {
                        level = LogLevel.ALL
                    }
                    install(SSE)
                    defaultRequest {
                        // Evaluated for every request: follows whoever the test is logged in as. A request that
                        // sets its own Authorization header keeps it.
                        accessToken?.let { headers.append(HttpHeaders.Authorization, "Bearer $it") }
                    }
                }

                if (shouldLogIn == LoginType.USER) loginAsFakeUser()
                else if (shouldLogIn == LoginType.ADMIN) loginAsFakeAdminUser()

                val context = ApplicationTestContext(dib)
                block(context)
            }
        } finally {
            accessToken = null

            // Re-enable push for tests that require it
            Push.disable = false

            Database.clear()
            // RedisStoreMap.fromEnv falls back to a process-lifetime InMemoryStoreMap when no Redis is configured
            // (the case in tests), so entries written by handleIfModified/notifyUpdateForEntity in one test would
            // otherwise leak into every later test's If-Modified-Since checks for the same entity type.
            RedisStoreMap.fromEnv.clear()
            finally()

            resetTimeFunctions()
        }
    }

    /**
     * The access token the test client sends with every request, see [loginAs].
     */
    private var accessToken: String? = null

    /**
     * Logs the test client in as [user], creating it if it doesn't exist, with a real session (see [AuthTokens]).
     * Replaces whoever it was logged in as before.
     */
    fun loginAs(user: StubUser) {
        val tokens = Database {
            AuthTokens.startSession(user.provideEntity(), AuthSessionMethod.PASSWORD, ClientInfo(null, "test"))
        }
        accessToken = tokens.accessToken
        System.err.println("Logged in as ${user.sub}")
    }

    /** Makes the test client send no access token anymore. */
    fun logout() {
        accessToken = null
    }

    @Suppress("UnusedReceiverParameter")
    fun ApplicationTestBuilder.loginAsFakeUser() = loginAs(FakeUser)

    @Suppress("UnusedReceiverParameter")
    fun ApplicationTestBuilder.loginAsFakeAdminUser() = loginAs(FakeAdminUser)

    @Suppress("UnusedReceiverParameter")
    fun ApplicationTestBuilder.loginAsFakeUser2() = loginAs(FakeUser2)
}
