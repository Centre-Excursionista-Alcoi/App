package org.centrexcursionistalcoi.app

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.centrexcursionistalcoi.app.ResourcesUtils.bytesFromResource
import org.centrexcursionistalcoi.app.database.entity.FileEntity

class ApplicationTest: ApplicationTestBase() {

    @Test
    fun testRoot() = testApplication {
        application {
            module()
        }
        val response = client.get("/")
        response.assertStatusCode(HttpStatusCode.OK)
        assertEquals("Hello! The Centre Excursionista d'Alcoi API is running.", response.bodyAsText())
    }

    @Test
    fun testDownload() = runApplicationTest(
        databaseInitBlock = {
            FileEntity.new {
                name = "square.png"
                type = "image/png"
                bytes = bytesFromResource("/square.png")
            }.id.value
        }
    ) { context ->
        val fileId = context.dibResult
        assertNotNull(fileId)

        // unknown is not a valid UUID
        client.get("/download/unknown").assertStatusCode(HttpStatusCode.BadRequest)

        // non-existing UUID
        client.get("/download/00000000-0000-0000-0000-000000000000").assertStatusCode(HttpStatusCode.NotFound)

        val rawFile = bytesFromResource("/square.png")

        client.get("/download/$fileId").let { response ->
            response.assertStatusCode(HttpStatusCode.OK)
            assertEquals(ContentType.Image.PNG, response.contentType())
            assertContentEquals(rawFile, response.bodyAsBytes())
        }
    }
}
