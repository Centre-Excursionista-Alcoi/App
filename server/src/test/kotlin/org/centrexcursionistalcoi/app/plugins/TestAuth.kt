package org.centrexcursionistalcoi.app.plugins

import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters
import io.ktor.util.appendAll
import kotlin.test.Test
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertError
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.error.Error

class TestAuth: ApplicationTestBase() {
    private val parameters = mapOf(
        "nif" to "87654321X",
        "password" to "TestPassword123",
    )

    @Test
    fun test_registration_contentType() = runApplicationTest {
        val response = client.post("/register")
        response.assertStatusCode(HttpStatusCode.BadRequest)
    }

    @Test
    fun test_registration_missingFields() = runApplicationTest {
        client.submitForm("/register").assertStatusCode(HttpStatusCode.BadRequest)
        for ((key) in parameters) {
            client.submitForm(
                "/register",
                parameters { appendAll(parameters.filterKeys { it != key }) },
            ).apply {
                assertError(Error.MissingArgument(key))
            }
        }
    }

    @Test
    fun test_registration_invalidNIF() = runApplicationTest {
        client.submitForm(
            "/register",
            parameters { appendAll(parameters + ("nif" to "invalid")) },
        ).apply {
            assertError(Error.InvalidArgument("nif"))
        }
    }

    @Test
    fun test_registration_invalidPassword() = runApplicationTest {
        val passwords = listOf("", "short", "alllowercase", "ALLUPPERCASE", "1234567890", "NoNumbers", "nouppercase1", "NOLOWERCASE1")
        for (password in passwords) {
            client.submitForm(
                "/register",
                parameters { appendAll(parameters + ("password" to password)) },
            ).apply {
                assertError(Error.PasswordNotSafeEnough())
            }
        }
    }

    @Test
    fun test_registration_success() = runApplicationTest {
        client.submitForm(
            "/register",
            parameters { appendAll(parameters) },
        ).apply {
            assertStatusCode(HttpStatusCode.Found)
        }
    }


    @Test
    fun test_login_contentType() = runApplicationTest {
        val response = client.post("/login")
        response.assertStatusCode(HttpStatusCode.BadRequest)
    }

    @Test
    fun test_login_missingFields() = runApplicationTest {
        client.submitForm("/login").assertStatusCode(HttpStatusCode.BadRequest)
        for ((key) in parameters) {
            client.submitForm(
                "/login",
                parameters { appendAll(parameters.filterKeys { it != key }) },
            ).apply {
                assertError(Error.IncorrectPasswordOrNIF())
            }
        }
    }

    @Test
    fun test_login_wrongNIF() = runApplicationTest {
        client.submitForm(
            "/login",
            parameters { appendAll(parameters + ("nif" to "invalid")) },
        ).apply {
            assertError(Error.IncorrectPasswordOrNIF())
        }
    }

    @Test
    fun test_login_wrongPassword() = runApplicationTest {
        client.submitForm(
            "/login",
            parameters { appendAll(parameters + ("password" to "invalid")) },
        ).apply {
            assertError(Error.IncorrectPasswordOrNIF())
        }
    }

    @Test
    fun test_login_success() = runApplicationTest {
        client.submitForm(
            "/login",
            parameters { appendAll(parameters) },
        ).apply {
            assertStatusCode(HttpStatusCode.Found)
        }
    }
}
