package org.centrexcursionistalcoi.app.database.migrations

import io.ktor.http.ContentType
import java.util.UUID
import org.centrexcursionistalcoi.app.database.FileReferences
import org.centrexcursionistalcoi.app.database.table.Files
import org.centrexcursionistalcoi.app.storage.FileStorage
import org.centrexcursionistalcoi.app.storage.FileStorageProvider
import org.centrexcursionistalcoi.app.utils.detectFileType
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.statements.jdbc.JdbcConnectionImpl
import org.slf4j.LoggerFactory

/**
 * Migration V11:
 * - The contents of files are moved from the `bytes` column of `files` to the file storage (Cloudflare R2), and
 *   the column is dropped. Each file's contents are stored at `files/<id>` (recorded in `objectKey`), and their
 *   size in `size`.
 * - Files not referenced by anything are deleted first, instead of being moved.
 *
 * Everything runs in the migration's transaction: if anything fails (e.g. the storage is not available), nothing
 * changes in the database, and the migration runs again on the next start. Contents already stored are overwritten
 * then, since their keys don't change.
 */
object V11 : DatabaseMigration {
    private val logger = LoggerFactory.getLogger(V11::class.java)

    override val from: Int = 10
    override val to: Int = 11

    context(tr: JdbcTransaction)
    override fun migrate() = migrate(FileStorageProvider.current)

    context(tr: JdbcTransaction)
    internal fun migrate(storage: FileStorage) {
        val conn = (tr.connection as JdbcConnectionImpl).connection
        val files = tr.identity(Files)
        val objectKey = tr.identity(Files.objectKey)
        val size = tr.identity(Files.size)
        val type = tr.identity(Files.type)

        val hasBytes = conn.metaData.getColumns(null, null, Files.tableName, "bytes").use { it.next() }
        if (!hasBytes) {
            logger.info("V11: files have no contents in the database, nothing to move")
            return
        }

        tr.exec("ALTER TABLE $files ADD COLUMN $objectKey VARCHAR(255) NULL, ADD COLUMN $size BIGINT NULL")

        // Delete the files nothing references, instead of moving them
        val unreferenced = FileReferences.columns.joinToString(" AND ") { column ->
            "NOT EXISTS (SELECT 1 FROM ${tr.identity(column.table)} r WHERE r.${tr.identity(column)} = $files.id)"
        }
        val deleted = conn.createStatement().use { it.executeUpdate("DELETE FROM $files WHERE $unreferenced") }
        logger.info("V11: deleted $deleted files not referenced by anything")

        val ids = conn.createStatement().use { statement ->
            statement.executeQuery("SELECT id FROM $files ORDER BY id").use { result ->
                buildList { while (result.next()) add(result.getObject(1, UUID::class.java)) }
            }
        }
        val totalBytes = conn.createStatement().use { statement ->
            statement.executeQuery("SELECT COALESCE(SUM(OCTET_LENGTH(bytes)), 0) FROM $files").use { it.next(); it.getLong(1) }
        }
        logger.info("V11: moving ${ids.size} files (${totalBytes.humanReadableBytes()}) to ${storage.description}")
        if (ids.isNotEmpty()) storage.checkAvailable()

        // One at a time, never loading the contents of every file at once
        var movedBytes = 0L
        conn.prepareStatement("SELECT bytes, $type FROM $files WHERE id = ?").use { select ->
            conn.prepareStatement("UPDATE $files SET $objectKey = ?, $size = ?, $type = ? WHERE id = ?").use { update ->
                ids.forEachIndexed { index, id ->
                    select.setObject(1, id)
                    val (bytes, declaredType) = select.executeQuery().use { result ->
                        check(result.next()) { "V11: file $id disappeared while migrating" }
                        result.getBytes(1) to result.getString(2)
                    }
                    val contentType = declaredType?.takeUnless { it == ContentType.Application.OctetStream.toString() }
                        ?: (detectFileType(bytes)?.contentType ?: ContentType.Application.OctetStream).toString()

                    val key = "files/$id"
                    storage.put(key, bytes, contentType)
                    val stored = storage.head(key) ?: error("V11: $key is missing right after storing it")
                    check(stored.size == bytes.size.toLong()) {
                        "V11: stored ${stored.size} bytes for $key, expected ${bytes.size}"
                    }

                    update.setString(1, key)
                    update.setLong(2, bytes.size.toLong())
                    update.setString(3, contentType)
                    update.setObject(4, id)
                    update.executeUpdate()

                    movedBytes += bytes.size
                    if ((index + 1) % 25 == 0 || index + 1 == ids.size) {
                        logger.info("V11: moved ${index + 1}/${ids.size} files (${movedBytes.humanReadableBytes()}/${totalBytes.humanReadableBytes()})")
                    }
                }
            }
        }

        val missing = conn.createStatement().use { statement ->
            statement.executeQuery("SELECT COUNT(*) FROM $files WHERE $objectKey IS NULL OR $size IS NULL").use { it.next(); it.getLong(1) }
        }
        check(missing == 0L) { "V11: $missing files were not moved" }

        tr.exec("ALTER TABLE $files ALTER COLUMN $objectKey SET NOT NULL, ALTER COLUMN $size SET NOT NULL")
        Files.indices
            .filter { it.columns == listOf(Files.objectKey) }
            .flatMap { it.createStatement() }
            .forEach { tr.exec(it) }
        tr.exec("ALTER TABLE $files DROP COLUMN bytes")

        logger.info("V11: done. Run VACUUM FULL on $files to give the space of the moved contents back to the system.")
    }

    private fun Long.humanReadableBytes(): String = when {
        this >= 1 shl 30 -> "%.1f GiB".format(this / (1 shl 30).toDouble())
        this >= 1 shl 20 -> "%.1f MiB".format(this / (1 shl 20).toDouble())
        this >= 1 shl 10 -> "%.1f KiB".format(this / (1 shl 10).toDouble())
        else -> "$this B"
    }
}
