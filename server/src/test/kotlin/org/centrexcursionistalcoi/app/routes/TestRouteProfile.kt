package org.centrexcursionistalcoi.app.routes

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.serialization.bodyAsJson
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class TestRouteProfile : ApplicationTestBase() {
    @Test
    fun test_notLoggedIn() = runApplicationTest {
        client.get("/profile").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun test_loggedIn() = runApplicationTest {
        loginAsFakeUser()

        client.get("/profile").apply {
            assertEquals(HttpStatusCode.OK, status)
            val response = bodyAsJson(ProfileResponse.serializer())
            assertEquals("user", response.username)
            assertEquals("user@example.com", response.email)
            assertContentEquals(listOf("user"), response.groups)
        }
    }
}
