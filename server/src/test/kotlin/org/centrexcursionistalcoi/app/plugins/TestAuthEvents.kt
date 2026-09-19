package org.centrexcursionistalcoi.app.plugins

import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertError
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.assertSuccess
import org.centrexcursionistalcoi.app.data.AuthEvent
import org.centrexcursionistalcoi.app.data.AuthEventType
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.table.AuthEvents
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.serialization.bodyAsJson
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.LoginType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    @Test
    fun test_auth_events_notLoggedIn() = runApplicationTest {
        client.get("/auth_events").apply {
            assertError(Error.NotLoggedIn())
        }
    }

    @Test
    fun test_auth_events_notAdmin() = runApplicationTest(
        shouldLogIn = LoginType.USER,
    ) {
        client.get("/auth_events").apply {
            assertError(Error.NotAnAdmin())
        }
    }

    @Test
    fun test_auth_events_admin_listsAndFiltersByEmail() = runApplicationTest {
        // A failed login attempt for FakeUser's email, and a failed registration for an unrelated email -- both
        // while unauthenticated, since /login short-circuits with 200 OK for an already-logged-in caller.
        client.submitForm(
            "/login",
            parameters { parameters.forEach { (key, value) -> append(key, value) } },
        ).assertStatusCode(HttpStatusCode.Unauthorized)

        client.submitForm(
            "/register",
            parameters { append("email", "other@example.com"); append("password", "TestPassword123") },
        ).assertStatusCode(HttpStatusCode.NotFound)

        loginAsFakeAdminUser()

        client.get("/auth_events") {
            parameter("email", FakeUser.EMAIL)
        }.apply {
            assertStatusCode(HttpStatusCode.OK)
            val events = bodyAsJson(ListSerializer(AuthEvent.serializer()))
            assertEquals(1, events.size)
            assertEquals(AuthEventType.LOGIN, events[0].type)
            assertEquals(FakeUser.EMAIL.uppercase(), events[0].email)
            assertTrue(!events[0].success)
        }
    }
}
