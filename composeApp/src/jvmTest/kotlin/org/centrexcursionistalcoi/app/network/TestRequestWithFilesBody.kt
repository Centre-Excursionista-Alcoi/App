package org.centrexcursionistalcoi.app.network

import androidx.room3.Room
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.data.Memory
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import org.centrexcursionistalcoi.app.database.AppDatabase
import org.centrexcursionistalcoi.app.database.MemoriesRepository
import org.centrexcursionistalcoi.app.database.getRoomDatabase
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.request.RequestWithFiles
import org.centrexcursionistalcoi.app.request.UpdateMemoryRequest

/**
 * Requests carrying files are sent as multipart, with each file's contents in a part of their own instead of encoded
 * in the JSON (see [RequestWithFiles]).
 */
class TestRequestWithFilesBody {
    private val original = _httpClient
    private var db: AppDatabase? = null

    @AfterTest
    fun tearDown() {
        _httpClient = original
        db?.close()
    }

    private val memory = Memory(
        id = Uuid.random(),
        place = null,
        members = emptyList(),
        externalUsers = null,
        text = "Memory text",
        sport = null,
        department = null,
        attachments = emptyList(),
        submittedBy = "user",
        from = ZonedDateTime.fromInstant(Instant.fromEpochMilliseconds(0), TimeZone.UTC),
        to = ZonedDateTime.fromInstant(Instant.fromEpochMilliseconds(0), TimeZone.UTC),
        pdf = null,
        lending = null,
    )

    private class SentRequest(val contentType: ContentType?, val body: ByteArray)

    /** Patches [memory] with [request], returning what was sent. */
    private suspend fun patch(request: UpdateMemoryRequest): SentRequest {
        var sent: SentRequest? = null
        _httpClient = HttpClient(MockEngine { call ->
            if (call.method == HttpMethod.Patch) {
                sent = SentRequest(call.body.contentType, call.body.toByteArray())
                respond("", HttpStatusCode.NoContent)
            } else {
                respond(
                    json.encodeToString(Memory.serializer(), memory),
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }
        })
        val database = getRoomDatabase(Room.inMemoryDatabaseBuilder<AppDatabase>(), Dispatchers.IO)
        db = database

        MemoriesRemoteRepository(MemoriesRepository(database)).update(memory.id, request, UpdateMemoryRequest.serializer())

        return assertNotNull(sent)
    }

    @Test
    fun `files with contents are sent as parts`() = runTest {
        val photo = ByteArray(4096) { (it % 251).toByte() }
        val removed = Uuid.random()
        val sent = patch(
            UpdateMemoryRequest(
                text = "Updated",
                attachments = listOf(
                    FileWithContext(photo, name = "photo.jpg", contentType = ContentType.Image.JPEG),
                    // Removing an attachment: no contents, stays in the JSON as is
                    FileWithContext(id = removed),
                ),
            )
        )

        assertTrue(sent.contentType!!.match(ContentType.MultiPart.FormData), "Sent as ${sent.contentType}")
        val body = sent.body.decodeToString()
        val boundary = sent.contentType.parameter("boundary")!!
        val parts = body.split("--$boundary").map { it.trim() }.filter { it.isNotEmpty() && it != "--" }

        // The request comes first, without the contents of the files
        val requestPart = parts.first()
        assertTrue("name=\"${RequestWithFiles.REQUEST_PART}\"" in requestPart, requestPart)
        val requestJson = json.parseToJsonElement(requestPart.substringAfter("\r\n\r\n")).jsonObject
        assertEquals("Updated", requestJson["text"]!!.jsonPrimitive.content)
        val (attachment, removal) = requestJson["attachments"]!!.jsonArray.map { it.jsonObject }
        assertEquals("file_0", attachment["part"]!!.jsonPrimitive.content)
        assertEquals("", attachment["bytes"]?.jsonPrimitive?.content.orEmpty())
        assertFalse("part" in removal)
        assertEquals(removed.toString(), removal["id"]!!.jsonPrimitive.content)

        // Then the file, as is (not encoded), with a file name so that the server streams it
        val filePart = parts[1]
        assertTrue("name=\"file_0\"" in filePart, filePart)
        assertTrue("filename=photo.jpg" in filePart, filePart)
        assertTrue("Content-Type: image/jpeg" in filePart, filePart)
        assertEquals(2, parts.size)
        val rawStart = sent.body.indexOf(photo.copyOf(64))
        assertTrue(rawStart >= 0, "The file must be sent as is")
    }

    @Test
    fun `requests without contents are sent as JSON`() = runTest {
        val removed = Uuid.random()
        val sent = patch(UpdateMemoryRequest(text = "Updated", attachments = listOf(FileWithContext(id = removed))))

        assertTrue(sent.contentType!!.match(ContentType.Application.Json), "Sent as ${sent.contentType}")
        val request = json.decodeFromString(UpdateMemoryRequest.serializer(), sent.body.decodeToString())
        assertEquals(removed, request.attachments!!.single().id)
    }

    private fun ByteArray.indexOf(other: ByteArray): Int {
        outer@ for (i in 0..size - other.size) {
            for (j in other.indices) if (this[i + j] != other[j]) continue@outer
            return i
        }
        return -1
    }
}
