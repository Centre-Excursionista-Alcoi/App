package org.centrexcursionistalcoi.app

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.Database.TEST_URL
import kotlin.test.*

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
    fun testPosts() = runTest {
        Database.init(TEST_URL)

        testApplication {
            application {
                module()
            }

            val response = client.get("/posts")
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }
}
