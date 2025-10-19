package org.centrexcursionistalcoi.app.plugins

import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.util.appendAll
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlin.test.Test
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertBody
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.json

class TestAuth: ApplicationTestBase() {
    private val parameters = mapOf(
        "username" to "testuser",
        "name" to "Test User",
        "email" to "mail@example.com",
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
                assertStatusCode(HttpStatusCode.BadRequest)
                assertBody { body: String -> body.startsWith("Missing") }
            }
        }
    }

    @Test
    fun test_registration_invalidEmail() = runApplicationTest {
        client.submitForm(
            "/register",
            parameters { appendAll(parameters + ("email" to "invalid")) },
        ).apply {
            assertStatusCode(HttpStatusCode.BadRequest)
            assertBody { body: String -> body.startsWith("Invalid email format") }
        }
    }

    @Test
    fun test_registration_invalidUsername() = runApplicationTest {
        val usernames = listOf("", "sh", "thisusernameiswaytoolongtobeaccepted", $$"invalid$username", "user name")
        for (username in usernames) {
            client.submitForm(
                "/register",
                parameters { appendAll(parameters + ("username" to username)) },
            ).apply {
                assertStatusCode(HttpStatusCode.BadRequest)
                assertBody { body: String -> body.startsWith("Invalid username format") }
            }
        }
    }

    @Test
    fun test_registration_invalidName() = runApplicationTest {
        val names = listOf("", "thisnameiswaytoolongtobeaccepted".repeat(4))
        for (name in names) {
            client.submitForm(
                "/register",
                parameters { appendAll(parameters + ("name" to name)) },
            ).apply {
                assertStatusCode(HttpStatusCode.BadRequest)
                assertBody { body: String -> body.startsWith("Invalid name format") }
            }
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
                assertStatusCode(HttpStatusCode.BadRequest)
                assertBody { body: String -> body.startsWith("Password must be") }
            }
        }
    }

    @Test
    fun test_registration_success() = runApplicationTest {
        externalServices {
            hosts("https://auth.example.com") {
                install(ContentNegotiation) {
                    json(json)
                }
                routing {
                    post("/api/v3/core/users/") {
                        call.respondText(
                            """
                                {
                                  "pk": 0,
                                  "username": "string",
                                  "name": "string",
                                  "is_active": true,
                                  "last_login": "2024-07-29T15:51:28.071Z",
                                  "date_joined": "2024-07-29T15:51:28.071Z",
                                  "is_superuser": true,
                                  "groups": [
                                    "3fa85f64-5717-4562-b3fc-2c963f66afa6"
                                  ],
                                  "groups_obj": [
                                    {
                                      "pk": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                      "num_pk": 0,
                                      "name": "string",
                                      "is_superuser": true,
                                      "parent": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                      "parent_name": "string",
                                      "attributes": {}
                                    }
                                  ],
                                  "email": "user@example.com",
                                  "avatar": "string",
                                  "attributes": {},
                                  "uid": "string",
                                  "path": "string",
                                  "type": "internal",
                                  "uuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                  "password_change_date": "2024-07-29T15:51:28.071Z",
                                  "last_updated": "2024-07-29T15:51:28.071Z"
                                }
                            """.trimIndent(),
                            ContentType.Application.Json,
                            HttpStatusCode.Created
                        )
                    }
                    post("/api/v3/core/users/{pk}/set_password/") {
                        call.respond(HttpStatusCode.NoContent)
                    }
                }
            }
        }

        try {
            mockkStatic(::getAuthHttpClient)
            every { getAuthHttpClient() } returns client

            client.submitForm(
                "/register",
                parameters { appendAll(parameters) },
            ).apply {
                assertStatusCode(HttpStatusCode.Created)
            }
        } finally {
            unmockkStatic(::getAuthHttpClient)
        }
    }
}
