package org.centrexcursionistalcoi.app.security

import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.PostgresTestBase
import org.centrexcursionistalcoi.app.database.table.AuthSessionMethod
import org.centrexcursionistalcoi.app.test.FakeUser
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The token queries on Postgres, which the H2 database of the other tests can't stand in for: row locking
 * (`SELECT ... FOR UPDATE`) and the new tables' constraints.
 */
class TestAuthTokensPostgres : PostgresTestBase() {
    @Test
    fun test_sessionLifecycle() {
        AES.initForTests()
        Database.init()

        val user = Database { transaction { FakeUser.provideEntity() } }
        val client = ClientInfo("127.0.0.1", "test")

        val first = Database { AuthTokens.startSession(user, AuthSessionMethod.PASSWORD, client) }
        assertEquals(FakeUser.SUB, assertNotNull(AuthTokens.resolveAccessToken(first.accessToken)).userSession.sub)

        val second = assertIs<RefreshResult.Success>(Database { AuthTokens.refresh(first.refreshToken) }).tokens
        assertNotNull(AuthTokens.resolveAccessToken(second.accessToken))

        // Reusing the first token after its successor was used revokes the session.
        assertIs<RefreshResult.Success>(Database { AuthTokens.refresh(second.refreshToken) })
        assertIs<RefreshResult.Reused>(Database { AuthTokens.refresh(first.refreshToken) })
        assertNull(AuthTokens.resolveAccessToken(second.accessToken))
    }
}
