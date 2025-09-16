package org.centrexcursionistalcoi.app

import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.cookies.cookies
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.setCookie
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.Database.TEST_URL
import org.centrexcursionistalcoi.app.database.entity.Department
import org.centrexcursionistalcoi.app.database.entity.File
import org.centrexcursionistalcoi.app.database.entity.Post
import org.centrexcursionistalcoi.app.database.table.Departments
import org.centrexcursionistalcoi.app.database.table.Files
import org.centrexcursionistalcoi.app.database.table.Posts
import org.centrexcursionistalcoi.app.database.utils.insert
import org.centrexcursionistalcoi.app.plugins.UserSession
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.plugins.json
import org.centrexcursionistalcoi.app.serialization.bodyAsJson
import org.centrexcursionistalcoi.app.serialization.getBoolean
import org.centrexcursionistalcoi.app.serialization.getString
import org.centrexcursionistalcoi.app.serialization.list
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        application {
            module()
        }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Ktor: ${Greeting().greet()}", response.bodyAsText())
    }

    @Test
    fun testDepartments() = runApplicationTest(
        databaseInitBlock = {
            Department.insert {
                it[Departments.displayName] = "Example Department"
            }
            val imageFile = File.insert {
                it[Files.name] = "square.png"
                it[Files.type] = "image/png"
                it[Files.data] = this::class.java.getResourceAsStream("/square.png").readBytes()
            }
            Department.insert {
                it[Departments.displayName] = "Image Department"
                it[Departments.imageFile] = imageFile.id
            }
        }
    ) {
        client.get("/departments").let { response ->
            assertEquals(HttpStatusCode.OK, response.status)

            val departments = response.bodyAsJson(JsonObject.serializer().list())
            println("Departments: $departments")
            departments[0].apply {
                assertEquals("Example Department", getString("displayName"))
            }
            departments[1].apply {
                assertEquals("Image Department", getString("displayName"))
                assertNotNull(getString("imageFile"), "Image file URL should not be null")
            }
        }
    }

    @Test
    fun testPosts() = runApplicationTest(
        databaseInitBlock = {
            val department = Department.insert {
                it[Departments.displayName] = "Example Department"
            }
            Post.insert {
                it[Posts.title] = "Members Post"
                it[Posts.content] = "This is a members-only post."
                it[Posts.onlyForMembers] = true
                it[Posts.department] = department.id
            }
            Post.insert {
                it[Posts.title] = "Public Post"
                it[Posts.content] = "This is a public post."
                it[Posts.onlyForMembers] = false
                it[Posts.department] = department.id
            }
        }
    ) {
        // Test non-authorized access, with no members-only posts
        client.get("/posts").let { response ->
            assertEquals(HttpStatusCode.OK, response.status)

            // Entities cannot be deserialized, use JsonObject
            val posts = response.bodyAsJson(JsonObject.serializer().list())
            assertEquals(1, posts.size)
            posts[0].apply {
                assertEquals("Public Post", getString("title"))
                assertEquals("This is a public post.", getString("content"))
                assertFalse(getBoolean("onlyForMembers"))
            }
        }

        // Log in as fake user
        loginAsFakeUser()

        // Test authorized access
        client.get("/posts").let { response ->
            assertEquals(HttpStatusCode.OK, response.status)

            val posts = response.bodyAsJson(JsonObject.serializer().list())
            assertEquals(2, posts.size)
            posts[0].apply {
                assertEquals("Members Post", getString("title"))
                assertEquals("This is a members-only post.", getString("content"))
                assertTrue(getBoolean("onlyForMembers"))
            }
            posts[1].apply {
                assertEquals("Public Post", getString("title"))
                assertEquals("This is a public post.", getString("content"))
                assertFalse(getBoolean("onlyForMembers"))
            }
        }
    }


    private fun runApplicationTest(
        databaseInitBlock: suspend R2dbcTransaction.() -> Unit = {},
        block: suspend ApplicationTestBuilder.() -> Unit
    ) = runTest {
        Database.init(TEST_URL)

        try {
            Database(databaseInitBlock)

            testApplication {
                application {
                    module(isTesting = true)

                    routing {
                        get("/test-login") {
                            // Simulate a user
                            val fakeUser = UserSession(
                                sub = "test-user-id-123",
                                username = "testuser",
                                email = "test@example.com",
                                groups = listOf("admins") // or ["users"]
                            )

                            call.sessions.set(fakeUser)
                            getUserSessionOrFail()

                            call.respondText("Logged in as ${fakeUser.username}")
                        }
                    }
                }
                client = createClient {
                    install(ContentNegotiation) {
                        json(json)
                    }
                    install(HttpCookies)
                }

                block()
            }
        } finally {
            Database.clear()
        }
    }

    private suspend fun ApplicationTestBuilder.loginAsFakeUser() {
        val response = client.get("/test-login")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("true", response.headers["CEA-LoggedIn"])
        assertNotNull(
            response.setCookie().find { it.name == UserSession.COOKIE_NAME },
            "Session cookie not found in response"
        )
        println("Cookies list: " + client.cookies("http://localhost/"))
        System.err.println("Logged in successfully!")
    }
}
