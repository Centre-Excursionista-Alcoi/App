package org.centrexcursionistalcoi.app.storage

import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.readBytes
import io.ktor.http.ContentType
import io.ktor.http.defaultForFileExtension
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.FileWithContext

object InMemoryFileAllocator {
    private val files = mutableMapOf<Uuid, Data>()

    class Data(
        val bytes: ByteArray,
        val contentType: ContentType? = null,
        val lastModified: Instant? = null,
        val id: Uuid? = null,
    ) {
        fun toFileWithContext(name: String? = null): FileWithContext = FileWithContext(bytes, name, contentType, lastModified, id)
    }

    fun put(bytes: ByteArray, uuid: Uuid? = null, contentType: ContentType? = null): Uuid {
        val uuid = uuid ?: Uuid.random()
        Napier.i { "Allocated a file of ${bytes.size} bytes at $uuid" }
        files[uuid] = Data(bytes, contentType, Clock.System.now(), uuid)
        return uuid
    }

    suspend fun put(platformFile: PlatformFile, uuid: Uuid? = null): Uuid {
        val bytes = platformFile.readBytes()
        val contentType = ContentType.defaultForFileExtension(platformFile.extension)
        return put(bytes, uuid, contentType)
    }

    fun get(uuid: Uuid): Data? = files[uuid]

    fun contains(uuid: Uuid): Boolean = files.containsKey(uuid)

    /**
     * Deletes the file associated with the given UUID from the in-memory storage.
     * @return The removed [Data] if it existed, or null if no file was associated with the UUID.
     */
    fun delete(uuid: Uuid) = files.remove(uuid)
}
