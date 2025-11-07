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
import org.centrexcursionistalcoi.app.assertSuccess
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.security.Passwords
import org.centrexcursionistalcoi.app.test.FakeUser
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class TestAuth: ApplicationTestBase() {
    private val parameters = mapOf(
        "nif" to FakeUser.NIF,
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
    fun test_registration_success() = runApplicationTest(
        databaseInitBlock = {
            FakeUser.provideEntity()
        }
    ) {
        client.submitForm(
            "/register",
            parameters { appendAll(parameters) },
        ).apply {
            assertSuccess()
        }
    }


    @Test
    fun test_login_contentType() = runApplicationTest {
        val response = client.post("/login")
        response.assertStatusCode(HttpStatusCode.BadRequest)
    }

    @Test
    fun test_login_missingFields() = runApplicationTest {
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
    fun test_login_success() = runApplicationTest(
        databaseInitBlock = {
            val entity = transaction { FakeUser.provideEntity() }
            entity.password = Passwords.hash(parameters.getValue("password").toCharArray())
        }
    ) {
        client.submitForm(
            "/login",
            parameters { appendAll(parameters) },
        ).apply {
            assertSuccess()
        }
    }
}
