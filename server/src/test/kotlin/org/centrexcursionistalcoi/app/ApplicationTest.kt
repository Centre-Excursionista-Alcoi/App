package org.centrexcursionistalcoi.app

import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.forms.append
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.setCookie
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import kotlinx.io.asOutputStream
import kotlinx.serialization.json.JsonObject
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.Database.TEST_URL
import org.centrexcursionistalcoi.app.database.entity.Department
import org.centrexcursionistalcoi.app.database.entity.File
import org.centrexcursionistalcoi.app.database.entity.Post
import org.centrexcursionistalcoi.app.database.table.Departments
import org.centrexcursionistalcoi.app.database.table.Files
import org.centrexcursionistalcoi.app.database.table.Posts
import org.centrexcursionistalcoi.app.database.utils.findBy
import org.centrexcursionistalcoi.app.database.utils.insert
import org.centrexcursionistalcoi.app.plugins.UserSession
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.serialization.bodyAsJson
import org.centrexcursionistalcoi.app.serialization.getBoolean
import org.centrexcursionistalcoi.app.serialization.getString
import org.centrexcursionistalcoi.app.serialization.list
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
    fun testDownload() = runApplicationTest(
        databaseInitBlock = {
            File.insert {
                it[Files.name] = "square.png"
                it[Files.type] = "image/png"
                it[Files.data] = bytesFromResource("/square.png")
            }.id.value
        }
    ) { context ->
        val fileId = context.dibResult
        assertNotNull(fileId)

        // unknown is not a valid UUID
        assertEquals(HttpStatusCode.BadRequest, client.get("/download/unknown").status)

        // non-existing UUID
        assertEquals(HttpStatusCode.NotFound, client.get("/download/00000000-0000-0000-0000-000000000000").status)

        val rawFile = bytesFromResource("/square.png")

        client.get("/download/$fileId").let { response ->
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ContentType.Image.PNG, response.contentType())
            assertContentEquals(rawFile, response.bodyAsBytes())
        }
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
                it[Files.data] = bytesFromResource("/square.png")
            }
            Department.insert {
                it[Departments.displayName] = "Image Department"
                it[Departments.imageFile] = imageFile.id
            }

            imageFile.id.value
        }
    ) { context ->
        val departmentImageId = context.dibResult
        assertNotNull(departmentImageId)

        client.get("/departments").let { response ->
            assertEquals(HttpStatusCode.OK, response.status)

            val departments = response.bodyAsJson(JsonObject.serializer().list())
            departments[0].apply {
                assertEquals("Example Department", getString("displayName"))
            }
            departments[1].apply {
                assertEquals("Image Department", getString("displayName"))
                assertEquals(departmentImageId.toString(), getString("imageFile"))
            }
        }
    }

    @Test
    fun testDepartment() = runApplicationTest(
        databaseInitBlock = {
            val imageFile = File.insert {
                it[Files.name] = "square.png"
                it[Files.type] = "image/png"
                it[Files.data] = bytesFromResource("/square.png")
            }
            Department.insert {
                it[Departments.displayName] = "Image Department"
                it[Departments.imageFile] = imageFile.id
            }
        }
    ) { context ->
        val department = context.dibResult
        assertNotNull(department)

        assertEquals(HttpStatusCode.NotFound, client.get("/departments/123").status)

        client.get("/departments/${department.id.value}").let { response ->
            assertEquals(HttpStatusCode.OK, response.status)

            val departmentResponse = response.bodyAsJson(JsonObject.serializer())
            assertEquals("Image Department", departmentResponse.getString("displayName"))
            assertEquals(department.imageFile.toString(), departmentResponse.getString("imageFile"))
        }
    }

    @Test
    fun testDepartmentCreate() = runApplicationTest { context ->
        // Test non-authorized access
        assertEquals(HttpStatusCode.Unauthorized, client.post("/departments").status)

        // Log in as fake user
        loginAsFakeUser()

        // Test authorized access, but not admin
        assertEquals(HttpStatusCode.Forbidden, client.post("/departments").status)

        // Log in as fake admin user
        (context.cookiesStorage as AcceptAllCookiesStorage).clear()
        loginAsFakeAdminUser()

        // Test authorized access as admin, but missing displayName
        assertEquals(HttpStatusCode.BadRequest, client.post("/departments").status)

        // Test authorized access as admin, with displayName
        val departmentId = client.submitFormWithBinaryData(
            url = "/departments",
            formData = formData {
                append("displayName", "New Department")
                append("image", "square.png", ContentType.Image.PNG) {
                    this::class.java.getResourceAsStream("/square.png")!!.copyTo(asOutputStream())
                }
            }
        ).let { response ->
            assertEquals(HttpStatusCode.Created, response.status)
            val location = response.headers[HttpHeaders.Location]
            assertNotNull(location, "Location header not found in response")
            val departmentId = location.substringAfterLast('/').toIntOrNull()
            assertNotNull(departmentId, "Location header is not a valid department ID: $location")
            departmentId
        }

        // Make sure the department was created
        val department = Database { Department.findBy { Departments.id eq departmentId } }
        assertNotNull(department, "Created department not found in database")
        assertEquals("New Department", department.displayName)

        val departmentImageId = department.imageFile
        assertNotNull(departmentImageId, "Created department has no image file")

        val departmentImageFile = Database { File.findBy { Files.id eq departmentImageId } }
        assertEquals("square.png", departmentImageFile?.name)
        assertEquals("image/png", departmentImageFile?.type)
        val rawFile = bytesFromResource("/square.png")
        assertContentEquals(rawFile, departmentImageFile?.data)
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
        block: suspend ApplicationTestBuilder.(ApplicationTestContext<Unit>) -> Unit
    ) = runApplicationTest({ }) { block(it) }

    private fun <DIB> runApplicationTest(
        databaseInitBlock: (suspend R2dbcTransaction.() -> DIB)? = null,
        block: suspend ApplicationTestBuilder.(ApplicationTestContext<DIB>) -> Unit
    ) = runTest {
        Database.init(TEST_URL)

        try {
            val dib = databaseInitBlock?.let { Database(it) }

            testApplication {
                application {
                    module(isTesting = true)

                    routing {
                        get("/test-login") {
                            // Simulate a user
                            val fakeUser = UserSession(
                                sub = "test-user-id-123",
                                username = "user",
                                email = "user@example.com",
                                groups = listOf("user")
                            )

                            call.sessions.set(fakeUser)
                            getUserSessionOrFail()

                            call.respondText("Logged in as ${fakeUser.username}")
                        }
                        get("/test-login-admin") {
                            // Simulate a user
                            val fakeUser = UserSession(
                                sub = "test-user-id-456",
                                username = "admin",
                                email = "admin@example.com",
                                groups = listOf(ADMIN_GROUP_NAME, "user")
                            )

                            call.sessions.set(fakeUser)
                            getUserSessionOrFail()

                            call.respondText("Logged in as ${fakeUser.username}")
                        }
                    }
                }
                val cookiesStorage = AcceptAllCookiesStorage()
                client = createClient {
                    install(ContentNegotiation) {
                        json(json)
                    }
                    install(HttpCookies) {
                        storage = cookiesStorage
                    }
                }

                val context = ApplicationTestContext(dib, cookiesStorage)
                block(context)
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
        System.err.println("Logged in successfully!")
    }

    private suspend fun ApplicationTestBuilder.loginAsFakeAdminUser() {
        val response = client.get("/test-login-admin")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("true", response.headers["CEA-LoggedIn"])
        assertNotNull(
            response.setCookie().find { it.name == UserSession.COOKIE_NAME },
            "Session cookie not found in response"
        )
        System.err.println("Logged in successfully!")
    }
    
    private fun bytesFromResource(path: String) = this::class.java.getResourceAsStream(path)!!.readBytes()
}
