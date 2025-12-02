package org.centrexcursionistalcoi.app.storage

import com.diamondedge.logging.logging
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.readBytes
import io.ktor.http.ContentType
import io.ktor.http.defaultForFileExtension
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.FileReference
import org.centrexcursionistalcoi.app.data.FileWithContext

object InMemoryFileAllocator {
    private val log = logging()

    private val files = mutableListOf<Data>()

    class Data(
        val bytes: ByteArray,
        val contentType: ContentType? = null,
        val lastModified: Instant? = null,
        val id: Uuid,
    ) {
        fun toFileWithContext(name: String? = null): FileWithContext = FileWithContext(bytes, name, contentType, lastModified, id)

        fun toFileReference() = FileReference(id)
    }

    fun put(bytes: ByteArray, uuid: Uuid? = null, contentType: ContentType? = null): Data {
        val uuid = uuid ?: Uuid.random()
        log.i { "Allocated a file of ${bytes.size} bytes at $uuid" }
        files += Data(bytes, contentType, Clock.System.now(), uuid)
        return Data(bytes, contentType, Clock.System.now(), uuid)
    }

    suspend fun put(platformFile: PlatformFile, uuid: Uuid? = null): Data {
        val bytes = platformFile.readBytes()
        val contentType = ContentType.defaultForFileExtension(platformFile.extension)
        return put(bytes, uuid, contentType)
    }

    fun get(uuid: Uuid): Data? = files.find { it.id == uuid }

    fun contains(uuid: Uuid): Boolean = get(uuid) != null

    /**
     * Deletes the file associated with the given UUID from the in-memory storage.
     * @return The removed [Data] if it existed, or null if no file was associated with the UUID.
     */
    fun delete(uuid: Uuid): Data? {
        val idx = files.indexOfFirst { it.id == uuid }
        return if (idx >= 0) {
            log.i { "Deallocated file at $uuid" }
            files.removeAt(idx)
        } else {
            null
        }
    }
}
