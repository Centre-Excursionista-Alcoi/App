package org.centrexcursionistalcoi.app.routes

import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import io.ktor.http.parametersOf
import java.time.Instant
import java.time.LocalDate
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.ResourcesUtils
import org.centrexcursionistalcoi.app.assertBadRequest
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.data.UserInsurance
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.LendingUserEntity
import org.centrexcursionistalcoi.app.database.entity.UserInsuranceEntity
import org.centrexcursionistalcoi.app.database.table.UserInsurances
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.serialization.bodyAsJson
import org.centrexcursionistalcoi.app.test.*
import org.jetbrains.exposed.v1.core.eq

class TestProfileRoutes : ApplicationTestBase() {
    @Test
    fun test_notLoggedIn() = ProvidedRouteTests.test_notLoggedIn("/profile")

    @Test
    fun test_loggedIn() = ProvidedRouteTests.test_loggedIn(
        "/profile",
        ProfileResponse.serializer()
    ) { response ->
        assertEquals(FakeUser.FULL_NAME, response.fullName)
        assertEquals("user@example.com", response.email)
        assertContentEquals(listOf("user"), response.groups)
        assertNull(response.lendingUser)
        assertTrue(response.insurances.isEmpty())
    }

    @Test
    fun test_conditionalHeaders_ifModifiedSince() {
        runApplicationTest(
            // GMT: Tuesday 20 October 2015 0:00:00
            mockNow = Instant.ofEpochSecond(1445299200),
            shouldLogIn = LoginType.USER,
        ) {
            client.get("/profile") {
                headers.append(HttpHeaders.IfModifiedSince, "Wed, 21 Oct 2015 07:28:00 GMT")
            }.apply {
                assertStatusCode(HttpStatusCode.NotModified)
            }
        }
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

        // Missing Phone
        client.submitForm(
            "/profile/lendingSignUp",
            parametersOf(lendingAllParameters.minus("phoneNumber")),
        ).assertBadRequest()

        // Missing Sports
        client.submitForm(
            "/profile/lendingSignUp",
            parametersOf(lendingAllParameters.minus("sports")),
        ).assertBadRequest()
    }

    @Test
    fun test_lendingSignUp_alreadySignedUp() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            LendingUserEntity.new {
                userSub = FakeUser.provideEntity()
                phoneNumber = "123456789"
                sports = listOf(Sports.CLIMBING, Sports.HIKING)
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
            assertEquals(FakeUser.SUB, lendingUser.userSub.id.value)
            assertEquals("123456789", lendingUser.phoneNumber)
            assertContentEquals(listOf(Sports.CLIMBING, Sports.HIKING), lendingUser.sports)
        }

        client.get("/profile").apply {
            val response = bodyAsJson(ProfileResponse.serializer())
            response.lendingUser?.let { lendingUser ->
                assertEquals(FakeUser.SUB, lendingUser.sub)
                assertEquals("123456789", lendingUser.phoneNumber)
                assertContentEquals(listOf(Sports.CLIMBING, Sports.HIKING), lendingUser.sports)
            }
        }
    }


    @Test
    fun test_insurances_notLoggedIn() = ProvidedRouteTests.test_notLoggedIn("/profile/insurances")

    @Test
    fun test_insurances_loggedIn() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            // Add some insurances for the user
            Database {
                UserInsuranceEntity.new {
                    userSub = FakeUser.provideEntity()
                    insuranceCompany = "FEMECV"
                    policyNumber = "POL123"
                    validFrom = LocalDate.of(2025, 1, 1)
                    validTo = LocalDate.of(2025, 12, 31)
                }
                UserInsuranceEntity.new {
                    userSub = FakeAdminUser.provideEntity()
                    insuranceCompany = "FEMECV"
                    policyNumber = "POL456"
                    validFrom = LocalDate.of(2025, 1, 1)
                    validTo = LocalDate.of(2025, 12, 31)
                }
            }
        }
    ) {
        client.get("/profile/insurances").apply {
            assertStatusCode(HttpStatusCode.OK)
            val response = bodyAsJson(ListSerializer(UserInsurance.serializer()))
            assertEquals(1, response.size)
            val insurance = response[0]
            assertEquals("FEMECV", insurance.insuranceCompany)
            assertEquals("POL123", insurance.policyNumber)
            assertEquals(LocalDate.of(2025, 1, 1).toKotlinLocalDate(), insurance.validFrom)
            assertEquals(LocalDate.of(2025, 12, 31).toKotlinLocalDate(), insurance.validTo)
        }
    }


    @Test
    fun test_insurances_post_notLoggedIn() = ProvidedRouteTests.test_notLoggedIn("/profile/insurances", HttpMethod.Post)

    @Test
    fun test_insurances_post_missingFields() = runApplicationTest(
        shouldLogIn = LoginType.USER,
    ) {
        // Missing all fields
        client.submitFormWithBinaryData("/profile/insurances", formData()).assertBadRequest()

        // Missing insuranceCompany
        client.submitFormWithBinaryData(
            "/profile/insurances",
            formData {
                append("policyNumber", "POL123")
                append("validFrom", "2025-01-01")
                append("validTo", "2025-12-31")
            }
        ).assertBadRequest()

        // Missing policyNumber
        client.submitFormWithBinaryData(
            "/profile/insurances",
            formData {
                append("insuranceCompany", "FEMECV")
                append("validFrom", "2025-01-01")
                append("validTo", "2025-12-31")
            }
        ).assertBadRequest()

        // Missing validFrom
        client.submitFormWithBinaryData(
            "/profile/insurances",
            formData {
                append("insuranceCompany", "FEMECV")
                append("policyNumber", "POL123")
                append("validTo", "2025-12-31")
            }
        ).assertBadRequest()

        // Missing validTo
        client.submitFormWithBinaryData(
            "/profile/insurances",
            formData {
                append("insuranceCompany", "FEMECV")
                append("policyNumber", "POL123")
                append("validFrom", "2025-01-01")
            }
        ).assertBadRequest()
    }

    @Test
    fun test_insurances_post_invalidDates() = runApplicationTest(
        shouldLogIn = LoginType.USER,
    ) {
        // Invalid validFrom
        client.submitFormWithBinaryData(
            "/profile/insurances",
            formData {
                append("insuranceCompany", "FEMECV")
                append("policyNumber", "POL123")
                append("validFrom", "invalid-date")
                append("validTo", "2025-12-31")

                val data = ResourcesUtils.bytesFromResource("/document.pdf")
                append(
                    key = "document",
                    value = Base64.UrlSafe.encode(data),
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Pdf.toString())
                    },
                )
            }
        ).assertBadRequest()

        // Invalid validTo
        client.submitFormWithBinaryData(
            "/profile/insurances",
            formData {
                append("insuranceCompany", "FEMECV")
                append("policyNumber", "POL123")
                append("validFrom", "2025-01-01")
                append("validTo", "invalid-date")

                val data = ResourcesUtils.bytesFromResource("/document.pdf")
                append(
                    key = "document",
                    value = Base64.UrlSafe.encode(data),
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Pdf.toString())
                    },
                )
            }
        ).assertBadRequest()
    }


    @Test
    fun test_insurances_post_correct() = runApplicationTest(
        shouldLogIn = LoginType.USER,
    ) {
        client.submitFormWithBinaryData(
            "/profile/insurances",
            formData {
                append("insuranceCompany", "FEMECV")
                append("policyNumber", "POL123")
                append("validFrom", "2025-01-01")
                append("validTo", "2025-12-31")

                val data = ResourcesUtils.bytesFromResource("/document.pdf")
                append(
                    key = "document",
                    value = Base64.UrlSafe.encode(data),
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Pdf.toString())
                    },
                )
            }
        ).apply {
            assertStatusCode(HttpStatusCode.NoContent)
        }

        // Check it is in the database
        Database {
            val insurances = UserInsuranceEntity.find { UserInsurances.userSub eq FakeUser.SUB }.toList()
            assertEquals(1, insurances.size)
            val insurance = insurances[0]
            assertEquals(FakeUser.SUB, insurance.userSub.id.value)
            assertEquals("FEMECV", insurance.insuranceCompany)
            assertEquals("POL123", insurance.policyNumber)
            assertEquals(LocalDate.of(2025, 1, 1), insurance.validFrom)
            assertEquals(LocalDate.of(2025, 12, 31), insurance.validTo)
        }
    }
}
