package org.centrexcursionistalcoi.app.routes

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.request
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.test.assertTrue
import kotlinx.serialization.DeserializationStrategy
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.serialization.bodyAsJson

object ProvidedRouteTests {
    private suspend fun HttpClient.request(url: String, method: HttpMethod, contentType: ContentType?, expectedStatusCode: HttpStatusCode) {
        request(url) {
            this.method = method
            if (contentType != null) contentType(contentType)
        }.apply {
            assertStatusCode(expectedStatusCode)
        }
    }

    context(base: ApplicationTestBase)
    fun test_notLoggedIn(baseUrl: String, method: HttpMethod = HttpMethod.Get, contentType: ContentType? = null) = base.runApplicationTest {
        assertTrue { baseUrl.startsWith('/') }

        client.request(baseUrl, method, contentType, HttpStatusCode.Unauthorized)
    }

    context(base: ApplicationTestBase)
    fun test_notLoggedIn_form(baseUrl: String) = base.runApplicationTest {
        assertTrue { baseUrl.startsWith('/') }

        client.submitFormWithBinaryData(baseUrl, emptyList()).apply {
            assertStatusCode(HttpStatusCode.Unauthorized)
        }
    }

    context(base: ApplicationTestBase)
    fun test_loggedIn_notAdmin(baseUrl: String, method: HttpMethod = HttpMethod.Get, contentType: ContentType? = null) = base.runApplicationTest {
        assertTrue { baseUrl.startsWith('/') }

        with(base) { loginAsFakeUser() }

        client.request(baseUrl, method, contentType, HttpStatusCode.Forbidden)
    }

    context(base: ApplicationTestBase)
    fun test_loggedIn_notAdmin_form(baseUrl: String) = base.runApplicationTest {
        assertTrue { baseUrl.startsWith('/') }

        with(base) { loginAsFakeUser() }

        client.submitFormWithBinaryData(baseUrl, listOf()).apply {
            assertStatusCode(HttpStatusCode.Forbidden)
        }
    }

    context(base: ApplicationTestBase)
    fun <T> test_loggedIn(
        baseUrl: String,
        deserializer: DeserializationStrategy<T>,
        isAdmin: Boolean = false,
        block: suspend (T) -> Unit
    ) = base.runApplicationTest {
        assertTrue { baseUrl.startsWith('/') }

        with(base) {
            if (isAdmin) loginAsFakeAdminUser()
            else loginAsFakeUser()
        }

        client.get(baseUrl).apply {
            assertStatusCode(HttpStatusCode.OK)
            val response = bodyAsJson(deserializer)
            block(response)
        }
    }
}
