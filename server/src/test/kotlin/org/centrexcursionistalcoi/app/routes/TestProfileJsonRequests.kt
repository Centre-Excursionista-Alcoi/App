package org.centrexcursionistalcoi.app.routes

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.parametersOf
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.ResourcesUtils
import org.centrexcursionistalcoi.app.assertError
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.data.UserInsurance
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.FCMRegistrationTokenEntity
import org.centrexcursionistalcoi.app.database.entity.LendingUserEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.request.CreateInsuranceRequest
import org.centrexcursionistalcoi.app.request.LendingSignUpRequest
import org.centrexcursionistalcoi.app.request.LinkFEMECVRequest
import org.centrexcursionistalcoi.app.request.RegisterFCMTokenRequest
import org.centrexcursionistalcoi.app.request.RequestWithFiles
import org.centrexcursionistalcoi.app.request.RevokeFCMTokenRequest
import org.centrexcursionistalcoi.app.serialization.bodyAsJson
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.FakeUser2
import org.centrexcursionistalcoi.app.test.LoginType

/**
 * The profile routes receiving JSON. The forms app versions that predate JSON send stay covered by
 * [TestProfileRoutes].
 */
class TestProfileJsonRequests : ApplicationTestBase() {
    private val pdf = ResourcesUtils.bytesFromResource("/document.pdf")
    private val png = ResourcesUtils.bytesFromResource("/image.png")

    private suspend fun <T> HttpClient.sendJson(
        url: String,
        serializer: KSerializer<T>,
        request: T,
        method: HttpMethod = HttpMethod.Post,
    ): HttpResponse = request(url) {
        this.method = method
        contentType(ContentType.Application.Json)
        setBody(json.encodeToString(serializer, request))
    }

    private fun insurance(documents: List<FileWithContext> = emptyList()) = CreateInsuranceRequest(
        insuranceCompany = "Rocalsub",
        policyNumber = "POL123",
        validFrom = LocalDate(2025, 1, 1),
        validTo = LocalDate(2025, 12, 31),
        documents = documents,
    )

    private suspend fun HttpClient.insurances(): List<UserInsurance> =
        get("/profile/insurances").bodyAsJson(ListSerializer(UserInsurance.serializer()))

    // ---- /profile/lendingSignUp ----

    @Test
    fun test_lendingSignUp_success() = runApplicationTest(shouldLogIn = LoginType.USER) {
        client.sendJson(
            "/profile/lendingSignUp",
            LendingSignUpRequest.serializer(),
            LendingSignUpRequest("123456789", listOf(Sports.CLIMBING, Sports.HIKING)),
        ).assertStatusCode(HttpStatusCode.Created)

        Database {
            val lendingUser = LendingUserEntity.all().single()
            assertEquals(FakeUser.SUB, lendingUser.userSub.id.value)
            assertEquals("123456789", lendingUser.phoneNumber)
            assertContentEquals(listOf(Sports.CLIMBING, Sports.HIKING), lendingUser.sports)
        }
    }

    @Test
    fun test_lendingSignUp_missingFields() = runApplicationTest(shouldLogIn = LoginType.USER) {
        client.sendJson(
            "/profile/lendingSignUp",
            LendingSignUpRequest.serializer(),
            LendingSignUpRequest(" ", listOf(Sports.CLIMBING)),
        ).assertError(Error.MissingArgument("phoneNumber"))
        client.sendJson(
            "/profile/lendingSignUp",
            LendingSignUpRequest.serializer(),
            LendingSignUpRequest("123456789", emptyList()),
        ).assertError(Error.MissingArgument("sports"))
        client.post("/profile/lendingSignUp") {
            contentType(ContentType.Application.Json)
            setBody("""{"phoneNumber":"123456789"}""")
        }.assertError(Error.MalformedRequest())
        client.post("/profile/lendingSignUp") {
            contentType(ContentType.Application.Json)
            setBody("""{"phoneNumber":"123456789","sports":["NOT_A_SPORT"]}""")
        }.assertError(Error.MalformedRequest())

        Database { assertTrue(LendingUserEntity.all().empty()) }
    }

    @Test
    fun test_lendingSignUp_legacyForm_invalidSport() = runApplicationTest(shouldLogIn = LoginType.USER) {
        client.submitForm(
            "/profile/lendingSignUp",
            parametersOf("phoneNumber" to listOf("123456789"), "sports" to listOf("CLIMBING,NOT_A_SPORT")),
        ).assertStatusCode(HttpStatusCode.BadRequest)
    }

    // ---- /profile/insurances ----

    @Test
    fun test_insurances_post_withoutDocuments() = runApplicationTest(shouldLogIn = LoginType.USER) {
        client.sendJson("/profile/insurances", CreateInsuranceRequest.serializer(), insurance())
            .assertStatusCode(HttpStatusCode.NoContent)

        val insurance = client.insurances().single()
        assertEquals("Rocalsub", insurance.insuranceCompany)
        assertEquals("POL123", insurance.policyNumber)
        assertEquals(LocalDate(2025, 1, 1), insurance.validFrom)
        assertEquals(LocalDate(2025, 12, 31), insurance.validTo)
        assertTrue(insurance.documents.isEmpty())
    }

    @Test
    fun test_insurances_post_missingFields() = runApplicationTest(shouldLogIn = LoginType.USER) {
        client.sendJson("/profile/insurances", CreateInsuranceRequest.serializer(), insurance().copy(insuranceCompany = ""))
            .assertError(Error.MissingArgument("insuranceCompany"))
        client.sendJson("/profile/insurances", CreateInsuranceRequest.serializer(), insurance().copy(policyNumber = " "))
            .assertError(Error.MissingArgument("policyNumber"))
        client.post("/profile/insurances") {
            contentType(ContentType.Application.Json)
            setBody("""{"insuranceCompany":"Rocalsub","policyNumber":"POL123","validFrom":"invalid-date","validTo":"2025-12-31"}""")
        }.assertError(Error.MalformedRequest())

        assertTrue(client.insurances().isEmpty())
    }

    @Test
    fun test_insurances_post_base64Documents() = runApplicationTest(shouldLogIn = LoginType.USER) {
        client.sendJson(
            "/profile/insurances",
            CreateInsuranceRequest.serializer(),
            insurance(
                listOf(
                    FileWithContext(pdf, "policy.pdf", ContentType.Application.Pdf),
                    FileWithContext(png, "card.png", ContentType.Image.PNG),
                )
            ),
        ).assertStatusCode(HttpStatusCode.NoContent)

        val insurance = client.insurances().single()
        assertEquals(2, insurance.documents.size)
        assertContentEquals(pdf, client.get("/download/${insurance.documents[0]}").bodyAsBytes())
        assertContentEquals(png, client.get("/download/${insurance.documents[1]}").bodyAsBytes())
    }

    @Test
    fun test_insurances_post_multipartDocuments_restricted() = runApplicationTest(shouldLogIn = LoginType.USER) {
        val request = insurance(listOf(FileWithContext(part = "file_0"), FileWithContext(part = "file_1")))
        client.post("/profile/insurances") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            RequestWithFiles.REQUEST_PART,
                            json.encodeToString(CreateInsuranceRequest.serializer(), request),
                            Headers.build { append(HttpHeaders.ContentType, ContentType.Application.Json.toString()) },
                        )
                        append("file_0", pdf, Headers.build { append(HttpHeaders.ContentDisposition, "filename=policy.pdf") })
                        append("file_1", png, Headers.build { append(HttpHeaders.ContentDisposition, "filename=card.png") })
                    }
                )
            )
        }.assertStatusCode(HttpStatusCode.NoContent)

        // Both documents, in order
        val insurance = client.insurances().single()
        assertEquals(2, insurance.documents.size)
        assertContentEquals(pdf, client.get("/download/${insurance.documents[0]}").bodyAsBytes())
        assertContentEquals(png, client.get("/download/${insurance.documents[1]}").bodyAsBytes())

        // Only the owner can download them
        Database { FakeUser2.provideEntity() }
        loginAsFakeUser2()
        for (document in insurance.documents) {
            client.get("/download/$document").assertStatusCode(HttpStatusCode.Forbidden)
        }
    }

    @Test
    fun test_insurances_post_missingPart() = runApplicationTest(shouldLogIn = LoginType.USER) {
        client.sendJson(
            "/profile/insurances",
            CreateInsuranceRequest.serializer(),
            insurance(listOf(FileWithContext(part = "file_0"))),
        ).assertError(Error.MalformedRequest())

        assertTrue(client.insurances().isEmpty())
    }

    // ---- /profile/femecvSync ----

    @Test
    fun test_femecvSync_missingCredentials() = runApplicationTest(shouldLogIn = LoginType.USER) {
        client.sendJson("/profile/femecvSync", LinkFEMECVRequest.serializer(), LinkFEMECVRequest("user", ""))
            .assertError(Error.FEMECVMissingCredentials())
    }

    @Test
    fun test_femecvSync_delete_withoutBody() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            FakeUser.provideEntity().apply {
                femecvUsername = "user"
                femecvPassword = "password"
            }
        },
    ) {
        client.delete("/profile/femecvSync").assertStatusCode(HttpStatusCode.NoContent)

        Database {
            val reference = UserReferenceEntity[FakeUser.SUB]
            assertEquals(null, reference.femecvUsername)
            assertEquals(null, reference.femecvPassword)
        }
    }

    // ---- /profile/fcmToken ----

    @Test
    fun test_fcmToken_registerAndRevokeByDevice() = runApplicationTest(shouldLogIn = LoginType.USER) {
        client.sendJson("/profile/fcmToken", RegisterFCMTokenRequest.serializer(), RegisterFCMTokenRequest("json-token", "device"))
            .assertStatusCode(HttpStatusCode.Created)
        Database {
            val token = assertNotNull(FCMRegistrationTokenEntity.findById("json-token"))
            assertEquals(FakeUser.SUB, token.user.sub.value)
            assertEquals("device", token.deviceId)
        }

        client.sendJson("/profile/fcmToken", RevokeFCMTokenRequest.serializer(), RevokeFCMTokenRequest("device"), HttpMethod.Delete)
            .assertStatusCode(HttpStatusCode.NoContent)
        Database { assertEquals(null, FCMRegistrationTokenEntity.findById("json-token")) }
    }

    @Test
    fun test_fcmToken_legacyForm() = runApplicationTest(shouldLogIn = LoginType.USER) {
        client.submitForm("/profile/fcmToken", parametersOf("token", "form-token"))
            .assertStatusCode(HttpStatusCode.Created)
        Database { assertNotNull(FCMRegistrationTokenEntity.findById("form-token")) }
    }

    @Test
    fun test_fcmToken_missingFields() = runApplicationTest(shouldLogIn = LoginType.USER) {
        client.sendJson("/profile/fcmToken", RegisterFCMTokenRequest.serializer(), RegisterFCMTokenRequest(""))
            .assertError(Error.FCMTokenIsRequired())
        client.sendJson("/profile/fcmToken", RevokeFCMTokenRequest.serializer(), RevokeFCMTokenRequest(""), HttpMethod.Delete)
            .assertError(Error.DeviceIdIsRequired())
    }
}
