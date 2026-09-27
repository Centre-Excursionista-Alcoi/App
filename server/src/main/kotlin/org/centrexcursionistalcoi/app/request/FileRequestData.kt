package org.centrexcursionistalcoi.app.request

import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.utils.io.core.Closeable
import io.ktor.utils.io.jvm.javaio.copyTo
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.coroutines.coroutineContext
import kotlin.io.encoding.Base64
import kotlin.io.path.deleteIfExists
import kotlin.io.path.outputStream
import kotlin.io.path.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.plugins.CallUploads
import org.centrexcursionistalcoi.app.security.FileReadWriteRules

/**
 * A file received in a request. Its contents are written to a temporary file as they arrive, instead of being held
 * in memory, and streamed from it to the storage by [newEntity].
 *
 * The temporary file is deleted by [close], and in any case once the call that received it has been handled (see
 * [CallUploads]).
 */
class FileRequestData : Closeable {
    companion object {
        /**
         * Converts a [FileWithContext] to a [FileRequestData].
         */
        suspend fun FileWithContext.toFileRequestData() = FileRequestData().apply {
            this.contentType = this@toFileRequestData.contentType
            this.originalFileName = this@toFileRequestData.name
            write { it.write(this@toFileRequestData.bytes) }
        }
    }

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
     * Populates this file data from the given [PartData.FormItem].
     *
     * Data will be provided as a Base64-encoded string in the form item.
     */
    suspend fun populate(partData: PartData.FormItem) {
        contentType = partData.contentType

        val filename = partData.headers[HttpHeaders.ContentDisposition]
            ?.let(ContentDisposition::parse)
            ?.parameters
            ?.find { it.name.equals("filename", true) }
            ?.value
        originalFileName = filename

        val value = Base64.UrlSafe.decode(partData.value)
        write { it.write(value) }
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
        val file = file
        return Database {
            val name = originalFileName ?: "unknown"
            if (file == null) {
                FileEntity.create(ByteArray(0), name, contentType, rules)
            } else {
                FileEntity.create(file, name, contentType, rules)
            }
        }.also { if (close) close() }
    }

    /**
     * Deletes the temporary file. Can be called more than once.
     */
    override fun close() {
        file?.deleteIfExists()
        file = null
    }
}
