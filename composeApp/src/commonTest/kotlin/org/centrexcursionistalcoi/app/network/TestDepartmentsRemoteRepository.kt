package org.centrexcursionistalcoi.app.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class TestDepartmentsRemoteRepository: ServerTestEnvironment() {
    private val mockEngine = MockEngine { request ->
        when (request.url.fullPath) {
            "/departments" -> respond(
                content = """
                        [
                            {"id": 1, "displayName": "Department 1", "imageFile": null},
                            {"id": 2, "displayName": "Department 2", "imageFile": "null"}
                        ]
                    """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json")
            )
            "/departments/1" -> respond(
                content = """{"id": 1, "displayName": "Department 1", "imageFile": null}""",
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json")
            )
            "/departments/2" -> respond(
                content = """{"id": 2, "displayName": "Department 2", "imageFile": "null"}""",
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json")
            )
            else -> respond(
                content = "Not Found",
                status = HttpStatusCode.NotFound,
                headers = headersOf("Content-Type", "text/plain")
            )
        }
    }

    @BeforeTest
    fun test_getAll() = runApplicationTest(
        engine = mockEngine,
    ) {
        val departments = DepartmentsRemoteRepository.getAll()
        assertEquals(2, departments.size)
        departments[0].let {
            assertEquals(1L, it.id)
            assertEquals("Department 1", it.displayName)
            assertEquals(null, it.image)
        }
        departments[1].let {
            assertEquals(2L, it.id)
            assertEquals("Department 2", it.displayName)
            assertEquals(null, it.image)
        }
    }
}
