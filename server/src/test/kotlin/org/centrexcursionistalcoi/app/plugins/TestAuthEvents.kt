package org.centrexcursionistalcoi.app.plugins

import io.ktor.client.request.forms.submitForm
import io.ktor.http.parameters
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertError
import org.centrexcursionistalcoi.app.assertSuccess
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.table.AuthEventType
import org.centrexcursionistalcoi.app.database.table.AuthEvents
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.test.FakeUser
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * These rows are only ever queried directly against the database (see `auth_events`'s KDoc) -- there's no API
 * endpoint exposing them -- so these tests only verify that requests get recorded, not any read path.
 */
class TestAuthEvents : ApplicationTestBase() {
    private val parameters = mapOf(
        "email" to FakeUser.EMAIL,
        "password" to "TestPassword123",
    )

    @Test
    fun test_login_failure_isRecorded() = runApplicationTest {
        client.submitForm(
            "/login",
            parameters { parameters.forEach { (key, value) -> append(key, value) } },
        ).apply {
            assertError(Error.IncorrectPasswordOrEmail())
        }

        val events = Database { AuthEvents.selectAll().where { AuthEvents.type eq AuthEventType.LOGIN }.toList() }
        assertEquals(1, events.size)
        events[0].let { row ->
            assertEquals(FakeUser.EMAIL.uppercase(), row[AuthEvents.email])
            assertEquals(false, row[AuthEvents.success])
            assertEquals(Error.IncorrectPasswordOrEmail().code, row[AuthEvents.errorCode])
        }
    }

    @Test
    fun test_register_success_isRecorded() = runApplicationTest(
        databaseInitBlock = {
            FakeUser.provideMemberEntity()
        }
    ) {
        client.submitForm(
            "/register",
            parameters { parameters.forEach { (key, value) -> append(key, value) } },
        ).apply {
            assertSuccess()
        }

        val events = Database { AuthEvents.selectAll().where { AuthEvents.type eq AuthEventType.REGISTER }.toList() }
        assertEquals(1, events.size)
        events[0].let { row ->
            assertEquals(FakeUser.EMAIL.uppercase(), row[AuthEvents.email])
            assertEquals(true, row[AuthEvents.success])
            assertNull(row[AuthEvents.errorCode])
        }
    }
}
