package org.centrexcursionistalcoi.app.plugins

import io.ktor.client.plugins.resources.post
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.util.appendAll
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertError
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.assertSuccess
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.href
import org.centrexcursionistalcoi.app.routes.Api
import org.centrexcursionistalcoi.app.security.Passwords
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.security.RegistrationCodes
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.centrexcursionistalcoi.app.database.UserPreferenceKey
import org.centrexcursionistalcoi.app.database.UserPreferenceStore
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import kotlin.text.toCharArray

class TestAuth: ApplicationTestBase() {
    private val parameters = mapOf(
        "email" to FakeUser.EMAIL,
        "password" to "TestPassword123",
        "code" to "000000",
    )

    @Test
    fun test_registration_contentType() = runApplicationTest {
        val response = client.post(Api.Register())
        response.assertStatusCode(HttpStatusCode.BadRequest)
    }

    @Test
    fun test_registration_missingFields() = runApplicationTest {
        client.submitForm(href(Api.Register())).assertStatusCode(HttpStatusCode.BadRequest)
        for ((key) in parameters) {
            client.submitForm(href(Api.Register()),
                parameters { appendAll(parameters.filterKeys { it != key }) },
            ).apply {
                assertError(Error.MissingArgument(key))
            }
        }
    }

    @Test
    fun test_registration_invalidEmail() = runApplicationTest {
        client.submitForm(href(Api.Register()),
            parameters { appendAll(parameters + ("email" to "invalid")) },
        ).apply {
            assertError(Error.InvalidArgument("email"))
        }
    }

    @Test
    fun test_registration_invalidPassword() = runApplicationTest {
        val passwords = listOf("", "short", "alllowercase", "ALLUPPERCASE", "1234567890", "NoNumbers", "nouppercase1", "NOLOWERCASE1")
        for (password in passwords) {
            client.submitForm(href(Api.Register()),
                parameters { appendAll(parameters + ("password" to password)) },
            ).apply {
                assertError(Error.PasswordNotSafeEnough())
            }
        }
    }

    @Test
    fun test_registration_memberDoesNotExist() = runApplicationTest {
        client.submitForm(href(Api.Register()),
            parameters { appendAll(parameters) },
        ).apply {
            assertError(Error.EmailNotFound())
        }
    }

    @Test
    fun test_registration_success() = runApplicationTest(
        databaseInitBlock = {
            FakeUser.provideMemberEntity()
        }
    ) {
        val code = RegistrationCodes.create(FakeUser.EMAIL.uppercase())
        client.submitForm(href(Api.Register()),
            parameters { appendAll(parameters + ("code" to code)) },
        ).apply {
            assertSuccess()
        }
    }

    @Test
    fun test_registration_storesTheLanguageOfTheRequest() = runApplicationTest(
        databaseInitBlock = {
            FakeUser.provideMemberEntity()
        }
    ) {
        val code = RegistrationCodes.create(FakeUser.EMAIL.uppercase())
        client.submitForm(
            href(Api.Register()),
            parameters { appendAll(parameters + ("code" to code)) },
        ) {
            header(HttpHeaders.AcceptLanguage, "ca-ES,ca;q=0.9,en;q=0.8")
        }.assertSuccess()

        val sub = transaction { UserReferenceEntity.findByEmail(FakeUser.EMAIL)!!.sub.value }
        assertEquals(Locale.forLanguageTag("ca-ES"), transaction { UserPreferenceStore[sub, UserPreferenceKey.Language] })
    }

    @Test
    fun test_registration_withoutLanguageStoresNone() = runApplicationTest(
        databaseInitBlock = {
            FakeUser.provideMemberEntity()
        }
    ) {
        val code = RegistrationCodes.create(FakeUser.EMAIL.uppercase())
        client.submitForm(href(Api.Register()), parameters { appendAll(parameters + ("code" to code)) }).assertSuccess()

        val sub = transaction { UserReferenceEntity.findByEmail(FakeUser.EMAIL)!!.sub.value }
        assertNull(transaction { UserPreferenceStore[sub, UserPreferenceKey.Language] })
    }

    @Test
    fun test_registration_wrongCode() = runApplicationTest(
        databaseInitBlock = {
            FakeUser.provideMemberEntity()
        }
    ) {
        val code = RegistrationCodes.create(FakeUser.EMAIL.uppercase())
        val wrongCode = if (code == "000000") "000001" else "000000"
        client.submitForm(href(Api.Register()),
            parameters { appendAll(parameters + ("code" to wrongCode)) },
        ).apply {
            assertError(Error.InvalidVerificationCode())
        }
    }

    @Test
    fun test_registration_withoutRequestingACode() = runApplicationTest(
        databaseInitBlock = {
            FakeUser.provideMemberEntity()
        }
    ) {
        client.submitForm(href(Api.Register()),
            parameters { appendAll(parameters) },
        ).apply {
            assertError(Error.InvalidVerificationCode())
        }
    }

    @Test
    fun test_login_passwordless_isNotAnError500() = runApplicationTest(
        databaseInitBlock = {
            val entity = transaction { FakeUser.provideEntity() }
            entity.password = ByteArray(0)
        }
    ) {
        client.submitForm(href(Api.Auth.Login()),
            parameters { appendAll(parameters) },
        ).apply {
            assertError(Error.PasswordNotSet())
        }
    }


    @Test
    fun test_login_empty() = runApplicationTest {
        val response = client.post(Api.Auth.Login())
        response.assertStatusCode(HttpStatusCode.BadRequest)
    }

    @Test
    fun test_login_missingFields() = runApplicationTest {
        for ((key) in parameters) {
            client.submitForm(href(Api.Auth.Login()),
                parameters { appendAll(parameters.filterKeys { it != key }) },
            ).apply {
                assertError(Error.IncorrectPasswordOrEmail())
            }
        }
    }

    @Test
    fun test_login_wrongEmail() = runApplicationTest {
        client.submitForm(href(Api.Auth.Login()),
            parameters { appendAll(parameters + ("email" to "invalid")) },
        ).apply {
            assertError(Error.IncorrectPasswordOrEmail())
        }
    }

    @Test
    fun test_login_wrongPassword() = runApplicationTest {
        client.submitForm(href(Api.Auth.Login()),
            parameters { appendAll(parameters + ("password" to "invalid")) },
        ).apply {
            assertError(Error.IncorrectPasswordOrEmail())
        }
    }

    @Test
    fun test_login_success() = runApplicationTest(
        databaseInitBlock = {
            val entity = transaction { FakeUser.provideEntity() }
            entity.password = Passwords.hash(parameters.getValue("password").toCharArray())
        }
    ) {
        client.submitForm(href(Api.Auth.Login()),
            parameters { appendAll(parameters) },
        ).apply {
            assertSuccess()
        }
    }
}
