package org.centrexcursionistalcoi.app.routes

import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.LendingUserEntity
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.serialization.bodyAsJson

class TestProfileRoutes : ApplicationTestBase() {
    @Test
    fun test_notLoggedIn() = runApplicationTest {
        client.get("/profile").apply {
            assertStatusCode(HttpStatusCode.Unauthorized)
        }
    }

    @Test
    fun test_loggedIn() = runApplicationTest {
        loginAsFakeUser()

        client.get("/profile").apply {
            assertStatusCode(HttpStatusCode.OK)
            val response = bodyAsJson(ProfileResponse.serializer())
            assertEquals("user", response.username)
            assertEquals("user@example.com", response.email)
            assertContentEquals(listOf("user"), response.groups)
            assertNull(response.lendingUser)
        }
    }

    @Test
    fun test_lendingSignUp_notLoggedIn() = runApplicationTest {
        client.post("/profile/lendingSignUp").apply {
            assertStatusCode(HttpStatusCode.Unauthorized)
        }
    }

    @Test
    fun test_lendingSignUp_invalidContentType() = runApplicationTest {
        loginAsFakeUser()

        client.post("/profile/lendingSignUp").apply {
            assertStatusCode(HttpStatusCode.BadRequest)
        }
    }

    @Test
    fun test_lendingSignUp_missingFields() = runApplicationTest {
        loginAsFakeUser()

        // Missing all fields
        client.submitForm("/profile/lendingSignUp").assertBadRequest()

        // Missing Phone
        client.submitForm(
            "/profile/lendingSignUp",
            parameters {
                append("nif", "12345678A")
            }
        ).assertBadRequest()

        // Missing NIF
        client.submitForm(
            "/profile/lendingSignUp",
            parameters {
                append("phoneNumber", "123456789")
            }
        ).assertBadRequest()
    }

    @Test
    fun test_lendingSignUp_alreadySignedUp() = runApplicationTest {
        Database {
            LendingUserEntity.new {
                userSub = FakeUser.SUB
                nif = "12345678A"
                phoneNumber = "123456789"
            }
        }

        loginAsFakeUser()

        client.submitForm(
            "/profile/lendingSignUp",
            parameters {
                append("nif", "87654321B")
                append("phoneNumber", "987654321")
            }
        ).apply {
            assertStatusCode(HttpStatusCode.Conflict)
        }
    }

    @Test
    fun test_lendingSignUp_success() = runApplicationTest {
        loginAsFakeUser()

        client.submitForm(
            "/profile/lendingSignUp",
            parameters {
                append("nif", "12345678A")
                append("phoneNumber", "123456789")
            }
        ).apply {
            assertStatusCode(HttpStatusCode.Created)
        }

        val lendingUsers = Database { LendingUserEntity.all().toList() }
        assertEquals(1, lendingUsers.size)
        lendingUsers.first().let { user ->
            assertEquals(FakeUser.SUB, user.userSub)
            assertEquals("12345678A", user.nif)
            assertEquals("123456789", user.phoneNumber)
        }

        client.get("/profile").apply {
            val response = bodyAsJson(ProfileResponse.serializer())
            response.lendingUser?.let { lendingUser ->
                assertEquals(FakeUser.SUB, lendingUser.sub)
                assertEquals("12345678A", lendingUser.nif)
                assertEquals("123456789", lendingUser.phoneNumber)
            }
        }
    }
}
