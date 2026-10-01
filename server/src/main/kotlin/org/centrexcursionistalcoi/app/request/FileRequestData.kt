package org.centrexcursionistalcoi.app.request

import io.ktor.http.ContentType
import io.ktor.http.content.PartData
import io.ktor.utils.io.core.Closeable
import io.ktor.utils.io.jvm.javaio.copyTo
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.coroutines.coroutineContext
import kotlin.io.path.deleteIfExists
import kotlin.io.path.outputStream
import kotlin.io.path.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.plugins.CallUploads
import org.centrexcursionistalcoi.app.security.FileReadWriteRules
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

/**
 * A file received in a request. Its contents are written to a temporary file as they arrive, instead of being held
 * in memory, and streamed from it to the storage by [newEntity].
 *
 * The temporary file is deleted by [close], and in any case once the call that received it has been handled (see
 * [CallUploads]).
 */
class FileRequestData : Closeable {
    var contentType: ContentType? = null
    var originalFileName: String? = null

    private var file: Path? = null

    /**
     * The number of bytes received.
     */
    var size: Long = 0
        private set

    fun isEmpty(): Boolean = size <= 0

    fun isNotEmpty(): Boolean = !isEmpty()

    private suspend fun write(writer: suspend (OutputStream) -> Unit) {
        val file = file ?: withContext(Dispatchers.IO) { Files.createTempFile("cea-upload-", ".tmp") }.also {
            file = it
            coroutineContext[CallUploads]?.register(this)
        }
        withContext(Dispatchers.IO) {
            file.outputStream(StandardOpenOption.APPEND).use { output -> writer(output) }
        }
        size = withContext(Dispatchers.IO) { Files.size(file) }
    }

    suspend fun populate(partData: PartData.FileItem) {
        contentType = partData.contentType
        originalFileName = partData.originalFileName
        write { output -> partData.provider().copyTo(output) }
    }

    /**
     * Reads the whole file into memory.
     */
    fun readBytes(): ByteArray = file?.readBytes() ?: ByteArray(0)

    /**
     * Creates a new [FileEntity] in the database with the data from this file and releases resources.
     * @param close Whether to close this file data after creating the entity. Defaults to true.
     * @param rules Read/write restrictions to record on the created file (see [FileEntity.rules]). Defaults to
     *   `null` (no restriction beyond requiring a logged-in session at download time) -- pass an explicit value
     *   for anything more sensitive than a shared/public asset (e.g. an insurance document or memory attachment).
     * @return The created [FileEntity].
     */
    fun newEntity(close: Boolean = true, rules: FileReadWriteRules? = null): FileEntity {
        return Database { store(originalFileName ?: "unknown", contentType, rules) }.also { if (close) close() }
    }

    /**
     * Stores this file (streamed from its temporary file) and creates a [FileEntity] for it, like
     * [FileEntity.create].
     */
    context(_: JdbcTransaction)
    fun store(name: String?, contentType: ContentType?, rules: FileReadWriteRules? = null, id: Uuid? = null): FileEntity {
        val file = file
        return if (file == null) {
            FileEntity.create(ByteArray(0), name, contentType, rules, id)
        } else {
            FileEntity.create(file, name, contentType, rules, id)
        }
    }

    /**
     * Replaces the contents of [entity] with this file, like [FileEntity.replaceContents].
     */
    context(_: JdbcTransaction)
    fun replaceContentsOf(entity: FileEntity, name: String?, contentType: ContentType?) {
        val file = file
        if (file == null) {
            entity.replaceContents(ByteArray(0), name, contentType)
        } else {
            entity.replaceContents(file, name, contentType)
        }
    }

    /**
     * Deletes the temporary file. Can be called more than once.
     */
    override fun close() {
        file?.deleteIfExists()
        file = null
    }
}
