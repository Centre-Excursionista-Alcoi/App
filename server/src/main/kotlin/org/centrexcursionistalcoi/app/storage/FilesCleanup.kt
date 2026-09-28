package org.centrexcursionistalcoi.app.storage

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration
import org.centrexcursionistalcoi.app.PeriodicWorker
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.FileReferences
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.now
import org.slf4j.LoggerFactory

/**
 * Deletes the files (and their contents) that nothing references anymore. Entities delete their files when they are
 * deleted: this catches whatever is left behind otherwise, e.g. a file uploaded for an entity whose creation failed
 * afterwards.
 *
 * Files are kept for [gracePeriod] first, since some are created before the rows that reference them.
 */
object FilesCleanup : PeriodicWorker(period = 24.hours) {
    private val logger = LoggerFactory.getLogger(FilesCleanup::class.java)

    private val gracePeriod = 1.days

    public override suspend fun run() {
        val threshold = now() - gracePeriod
        val deleted = Database {
            FileReferences.unreferencedFileIds(modifiedBefore = threshold)
                .mapNotNull { FileEntity.findById(it) }
                .onEach { it.delete() }
                .size
        }
        logger.info("Deleted $deleted files not referenced by anything.")
    }
}
