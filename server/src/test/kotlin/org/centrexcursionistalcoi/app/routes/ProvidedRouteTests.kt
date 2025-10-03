package org.centrexcursionistalcoi.app.routes

import io.ktor.client.request.get
import io.ktor.client.request.request
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlin.test.assertTrue
import kotlinx.serialization.DeserializationStrategy
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.serialization.bodyAsJson

object ProvidedRouteTests {
    context(base: ApplicationTestBase)
    fun test_notLoggedIn(baseUrl: String, method: HttpMethod = HttpMethod.Get) = base.runApplicationTest {
        assertTrue { baseUrl.startsWith('/') }

        client.request(baseUrl) {
            this.method = method
        }.apply {
            assertStatusCode(HttpStatusCode.Unauthorized)
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
