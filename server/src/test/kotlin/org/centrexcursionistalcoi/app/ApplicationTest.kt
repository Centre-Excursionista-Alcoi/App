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
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.entity.PostEntity
import org.centrexcursionistalcoi.app.database.table.Departments
import org.centrexcursionistalcoi.app.database.table.Files
import org.centrexcursionistalcoi.app.database.table.Posts
import org.centrexcursionistalcoi.app.plugins.UserSession
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.serialization.bodyAsJson
import org.centrexcursionistalcoi.app.serialization.getBoolean
import org.centrexcursionistalcoi.app.serialization.getString
import org.centrexcursionistalcoi.app.serialization.list
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
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
            FileEntity.new {
                name = "square.png"
                type = "image/png"
                data = bytesFromResource("/square.png")
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
            DepartmentEntity.new {
                displayName = "Example Department"
            }
            val imageFileEntity = FileEntity.new {
                name = "square.png"
                type = "image/png"
                data = bytesFromResource("/square.png")
            }
            DepartmentEntity.new {
                displayName = "Image Department"
                imageFile = imageFileEntity.id
            }

            imageFileEntity.id.value
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
            val imageFileEntity = FileEntity.new {
                name = "square.png"
                type = "image/png"
                data = bytesFromResource("/square.png")
            }
            DepartmentEntity.new {
                displayName = "Image Department"
                imageFile = imageFileEntity.id
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
            assertEquals(department.imageFile?.toString(), departmentResponse.getString("imageFile"))
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
        val department = Database { DepartmentEntity.findById(departmentId) }
        assertNotNull(department, "Created department not found in database")
        assertEquals("New Department", department.displayName)

        val departmentImageId = Database { department.imageFile }
        assertNotNull(departmentImageId, "Created department has no image file")

        val departmentImageFile = Database { FileEntity.findById(departmentImageId) }
        assertEquals("square.png", departmentImageFile?.name)
        assertEquals("image/png", departmentImageFile?.type)
        val rawFile = bytesFromResource("/square.png")
        assertContentEquals(rawFile, departmentImageFile?.data)
    }

    @Test
    fun testPosts() = runApplicationTest(
        databaseInitBlock = {
            val departmentEntity = DepartmentEntity.new {
                displayName = "Example Department"
            }
            PostEntity.new {
                title = "Members Post"
                content = "This is a members-only post."
                onlyForMembers = true
                department = departmentEntity.id
            }
            PostEntity.new {
                title = "Public Post"
                content = "This is a public post."
                onlyForMembers = false
                department = departmentEntity.id
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
        databaseInitBlock: (JdbcTransaction.() -> DIB)? = null,
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
