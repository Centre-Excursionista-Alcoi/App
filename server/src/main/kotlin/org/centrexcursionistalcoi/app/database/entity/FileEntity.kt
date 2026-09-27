package org.centrexcursionistalcoi.app.database.entity

import io.ktor.http.ContentType
import java.io.InputStream
import java.util.UUID
import kotlin.time.toKotlinInstant
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.database.FileReferences
import org.centrexcursionistalcoi.app.database.table.Files
import org.centrexcursionistalcoi.app.now
import org.centrexcursionistalcoi.app.security.FileReadWriteRules
import org.centrexcursionistalcoi.app.storage.FileObjectsTransactionHook
import org.centrexcursionistalcoi.app.storage.FileStorageProvider
import org.centrexcursionistalcoi.app.utils.detectFileType
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.slf4j.LoggerFactory

/**
 * A stored file. Its contents are in [FileStorageProvider.current], at [objectKey].
 *
 * Create files only with [create] (or [newFrom]), never with `new`: they upload the contents, and delete them again
 * if the transaction rolls back. [delete] deletes the contents once the transaction commits.
 */
class FileEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<FileEntity>(Files) {
        private val logger = LoggerFactory.getLogger(FileEntity::class.java)

        private fun newObjectKey() = "files/${UUID.randomUUID()}"

        /**
         * The type of a file: [declared] unless missing or generic, else detected from the first bytes of [contents].
         */
        fun resolveContentType(declared: ContentType?, contents: ByteArray): ContentType =
            declared?.takeUnless { it == ContentType.Application.OctetStream }
                ?: detectFileType(contents)?.contentType
                ?: ContentType.Application.OctetStream

        /**
         * Stores [bytes] and creates a file for them. If the transaction rolls back, the stored contents are deleted.
         * @param id The id to give the file. Ignored if a file already has it, so that a client can never overwrite
         * someone else's file by choosing its id.
         */
        context(tr: JdbcTransaction)
        fun create(
            bytes: ByteArray,
            name: String?,
            contentType: ContentType? = null,
            rules: FileReadWriteRules? = null,
            id: UUID? = null,
        ): FileEntity {
            val type = resolveContentType(contentType, bytes)
            val key = newObjectKey()
            FileStorageProvider.current.put(key, bytes, type.toString())
            FileObjectsTransactionHook.of(tr).deleteOnRollback(key)

            val fileId = id?.takeIf { findById(it) == null } ?: UUID.randomUUID()
            return new(fileId) {
                this.objectKey = key
                this.size = bytes.size.toLong()
                this.type = type.toString()
                this.name = name
                this.rules = rules
            }
        }

        context(_: JdbcTransaction)
        fun newFrom(withContext: FileWithContext, rules: FileReadWriteRules? = null) = create(
            bytes = withContext.bytes,
            name = withContext.name,
            contentType = withContext.contentType,
            rules = rules,
            id = withContext.id?.toJavaUuid(),
        )

        /**
         * Creates a file from [from], or deletes the file with its id if it has no contents.
         * @param ownedIds The ids of the files that belong to the entity being updated. Only those can be deleted:
         * any other id is ignored.
         * @param onDelete Called before deleting a file, e.g. to remove the rows that reference it.
         * @return The created file, or `null` if one was deleted (or nothing was done).
         */
        context(tr: JdbcTransaction)
        fun updateOrCreate(
            from: FileWithContext,
            ownedIds: Collection<UUID>,
            rules: FileReadWriteRules? = null,
            onDelete: JdbcTransaction.(FileEntity) -> Unit = {},
        ): FileEntity? {
            if (!from.isEmpty()) return newFrom(from, rules)

            // No bytes given, remove existing file
            val fileId = from.id?.toJavaUuid()
            if (fileId == null) {
                logger.warn("Asked to remove file from entity, but no id given, ignoring")
            } else if (fileId !in ownedIds) {
                logger.warn("Asked to remove file $fileId from entity, but it doesn't belong to it, ignoring")
            } else {
                findById(fileId)?.let {
                    logger.info("Removing file $fileId from entity...")
                    onDelete(tr, it)
                    it.delete()
                } ?: logger.warn("Asked to remove file $fileId from entity, but file does not exist, ignoring")
            }
            return null
        }

        /**
         * Deletes [file], unless it's still referenced by some row (see [FileReferences]). Used by the entities that
         * own files, when they are deleted.
         */
        /**
         * Deletes the [files] of an entity that has just been deleted, unless something else still references them.
         * Called from the `delete()` of the entities that own files.
         */
        fun deleteOwnedFiles(files: Collection<FileEntity>) {
            if (files.isEmpty()) return
            with(TransactionManager.current()) {
                files.forEach { deleteIfUnreferenced(it) }
            }
        }

        context(_: JdbcTransaction)
        fun deleteIfUnreferenced(file: FileEntity) {
            if (FileReferences.isReferenced(file.id.value)) {
                logger.warn("File ${file.id.value} is still referenced, not deleting it")
            } else {
                file.delete()
            }
        }
    }

    var objectKey by Files.objectKey
        private set
    var size by Files.size
        private set
    var type by Files.type
    var name by Files.name

    var lastModified by Files.lastModified

    var rules by Files.rules

    /**
     * The content type of the file. Defaults to `application/octet-stream` if not set.
     */
    val contentType: ContentType
        get() = type?.let(ContentType::parse) ?: ContentType.Application.OctetStream

    /**
     * Replaces the contents of this file, keeping its id. The new contents are stored at a new key, so that the old
     * ones stay in place until the transaction commits (and are deleted then).
     */
    context(tr: JdbcTransaction)
    fun replaceContents(bytes: ByteArray, name: String?, contentType: ContentType?) {
        val type = resolveContentType(contentType, bytes)
        val newKey = newObjectKey()
        val oldKey = objectKey
        FileStorageProvider.current.put(newKey, bytes, type.toString())
        val hook = FileObjectsTransactionHook.of(tr)
        hook.deleteOnRollback(newKey)

        this.objectKey = newKey
        this.size = bytes.size.toLong()
        this.type = type.toString()
        if (name != null) this.name = name
        this.lastModified = now()
        hook.deleteAfterCommit(oldKey)
    }

    /**
     * Deletes the file, and its contents once the transaction commits.
     */
    override fun delete() {
        val key = objectKey
        super.delete()
        FileObjectsTransactionHook.of(TransactionManager.current()).deleteAfterCommit(key)
    }

    /**
     * Opens the contents of the file. The caller must close the stream.
     */
    fun openStream(): InputStream = FileStorageProvider.current.open(objectKey)

    /**
     * Reads the whole contents of the file into memory.
     */
    fun readBytes(): ByteArray = FileStorageProvider.current.readBytes(objectKey)

    /**
     * The metadata of the file. [FileWithContext.bytes] is always empty: use [readBytes] for the contents.
     */
    fun toData(): FileWithContext = FileWithContext(
        id = id.value.toKotlinUuid(),
        name = name,
        bytes = ByteArray(0),
        contentType = contentType,
        lastModified = lastModified.toKotlinInstant(),
    )
}
