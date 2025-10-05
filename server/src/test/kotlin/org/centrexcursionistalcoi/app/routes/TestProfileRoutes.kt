package org.centrexcursionistalcoi.app.routes

import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters
import io.ktor.http.parametersOf
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertBadRequest
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.LendingUserEntity
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.serialization.bodyAsJson
import kotlin.test.assertNotNull

class TestProfileRoutes : ApplicationTestBase() {
    @Test
    fun test_notLoggedIn() = ProvidedRouteTests.test_notLoggedIn("/profile")

    @Test
    fun test_loggedIn() = ProvidedRouteTests.test_loggedIn(
        "/profile",
        ProfileResponse.serializer()
    ) { response ->
        assertEquals("user", response.username)
        assertEquals("user@example.com", response.email)
        assertContentEquals(listOf("user"), response.groups)
        assertNull(response.lendingUser)
    }

    private val lendingAllParameters = mapOf(
        Pair("fullName", listOf("John Doe")),
        Pair("nif", listOf("12345678A")),
        Pair("phoneNumber", listOf("123456789")),
        Pair("sports", listOf("CLIMBING,HIKING")),
        Pair("address", listOf("123 Main St")),
        Pair("postalCode", listOf("12345")),
        Pair("city", listOf("Anytown")),
        Pair("province", listOf("Anyprovince")),
        Pair("country", listOf("Anycountry")),
    )

    @Test
    fun test_lendingSignUp_notLoggedIn() = runApplicationTest {
        client.post("/profile/lendingSignUp").apply {
            assertStatusCode(HttpStatusCode.Unauthorized)
        }
    }

    @Test
    fun test_lendingSignUp_invalidContentType() = runApplicationTest(
        shouldLogIn = LoginType.USER
    ) {
        client.post("/profile/lendingSignUp").apply {
            assertStatusCode(HttpStatusCode.BadRequest)
        }
    }

    @Test
    fun test_lendingSignUp_missingFields() = runApplicationTest(
        shouldLogIn = LoginType.USER
    ) {
        // Missing all fields
        client.submitForm("/profile/lendingSignUp").assertBadRequest()

        // Missing Full name
        client.submitForm(
            "/profile/lendingSignUp",
            parametersOf(lendingAllParameters.minus("fullName")),
        ).assertBadRequest()

        // Missing Phone
        client.submitForm(
            "/profile/lendingSignUp",
            parametersOf(lendingAllParameters.minus("phoneNumber")),
        ).assertBadRequest()

        // Missing NIF
        client.submitForm(
            "/profile/lendingSignUp",
            parametersOf(lendingAllParameters.minus("nif")),
        ).assertBadRequest()

        // Missing Sports
        client.submitForm(
            "/profile/lendingSignUp",
            parametersOf(lendingAllParameters.minus("sports")),
        ).assertBadRequest()

        // Missing Address
        client.submitForm(
            "/profile/lendingSignUp",
            parametersOf(lendingAllParameters.minus("address")),
        ).assertBadRequest()

        // Missing Postal Code
        client.submitForm(
            "/profile/lendingSignUp",
            parametersOf(lendingAllParameters.minus("postalCode")),
        ).assertBadRequest()

        // Missing City
        client.submitForm(
            "/profile/lendingSignUp",
            parametersOf(lendingAllParameters.minus("city")),
        ).assertBadRequest()

        // Missing Province
        client.submitForm(
            "/profile/lendingSignUp",
            parametersOf(lendingAllParameters.minus("province")),
        ).assertBadRequest()

        // Missing Country
        client.submitForm(
            "/profile/lendingSignUp",
            parametersOf(lendingAllParameters.minus("country")),
        ).assertBadRequest()
    }

    @Test
    fun test_lendingSignUp_alreadySignedUp() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            LendingUserEntity.new {
                userSub = FakeUser.SUB
                fullName = "John Doe"
                nif = "12345678A"
                phoneNumber = "123456789"
                sports = listOf(Sports.CLIMBING, Sports.HIKING)
                address = "123 Main St"
                postalCode = "12345"
                city = "Anytown"
                province = "Anyprovince"
                country = "Anycountry"
            }
        }
    )  {
        client.submitForm(
            "/profile/lendingSignUp",
            parametersOf(lendingAllParameters),
        ).apply {
            assertStatusCode(HttpStatusCode.Conflict)
        }
    }

    @Test
    fun test_lendingSignUp_success() = runApplicationTest(
        shouldLogIn = LoginType.USER
    ) {
        client.submitForm(
            "/profile/lendingSignUp",
            parametersOf(lendingAllParameters),
        ).apply {
            assertStatusCode(HttpStatusCode.Created)
        }

        Database {
            val lendingUser = LendingUserEntity.all().firstOrNull()
            assertNotNull(lendingUser)
            assertEquals(FakeUser.SUB, lendingUser.userSub)
            assertEquals("John Doe", lendingUser.fullName)
            assertEquals("12345678A", lendingUser.nif)
            assertEquals("123456789", lendingUser.phoneNumber)
            assertContentEquals(listOf(Sports.CLIMBING, Sports.HIKING), lendingUser.sports)
            assertEquals("123 Main St", lendingUser.address)
            assertEquals("12345", lendingUser.postalCode)
            assertEquals("Anytown", lendingUser.city)
            assertEquals("Anyprovince", lendingUser.province)
            assertEquals("Anycountry", lendingUser.country)
        }

        client.get("/profile").apply {
            val response = bodyAsJson(ProfileResponse.serializer())
            response.lendingUser?.let { lendingUser ->
                assertEquals(FakeUser.SUB, lendingUser.sub)
                assertEquals("John Doe", lendingUser.fullName)
                assertEquals("12345678A", lendingUser.nif)
                assertEquals("123456789", lendingUser.phoneNumber)
                assertContentEquals(listOf(Sports.CLIMBING, Sports.HIKING), lendingUser.sports)
                assertEquals("123 Main St", lendingUser.address)
                assertEquals("12345", lendingUser.postalCode)
                assertEquals("Anytown", lendingUser.city)
                assertEquals("Anyprovince", lendingUser.province)
                assertEquals("Anycountry", lendingUser.country)
            }
        }
    }
}
