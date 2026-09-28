package org.centrexcursionistalcoi.app.database

import kotlin.time.Instant
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.database.table.Departments
import org.centrexcursionistalcoi.app.database.table.Events
import org.centrexcursionistalcoi.app.database.table.Files
import org.centrexcursionistalcoi.app.database.table.InventoryItemTypes
import org.centrexcursionistalcoi.app.database.table.Memories
import org.centrexcursionistalcoi.app.database.table.MemoriesFiles
import org.centrexcursionistalcoi.app.database.table.PostFiles
import org.centrexcursionistalcoi.app.database.table.UserInsuranceDocuments
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.notExists
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.select

/**
 * Every column that references [Files]. A file referenced by none of them is not used by anything, and can be
 * deleted.
 *
 * **Any new reference to [Files] must be added here**, or its files may be deleted while still in use.
 */
object FileReferences {
    val columns: List<Column<*>> = listOf(
        Departments.image,
        InventoryItemTypes.image,
        Events.image,
        Memories.pdf,
        MemoriesFiles.file,
        PostFiles.file,
        UserInsuranceDocuments.file,
    )

    @Suppress("UNCHECKED_CAST")
    private val Column<*>.asFileId get() = this as Column<EntityID<Uuid>>

    /**
     * Whether any row references the file with the given [fileId].
     */
    context(_: JdbcTransaction)
    fun isReferenced(fileId: Uuid): Boolean = columns.any { column ->
        column.table.select(column).where { column.asFileId eq fileId }.limit(1).any()
    }

    /**
     * Gets the ids of every file not referenced by any row.
     * @param modifiedBefore If not null, only files last modified before this instant are returned.
     */
    context(_: JdbcTransaction)
    fun unreferencedFileIds(modifiedBefore: Instant? = null): List<Uuid> {
        var condition: Op<Boolean> = columns
            .map<Column<*>, Op<Boolean>> { column -> notExists(column.table.select(column).where { column.asFileId eq Files.id }) }
            .reduce { acc, op -> acc and op }
        if (modifiedBefore != null) {
            condition = condition and (Files.lastModified less modifiedBefore)
        }
        return Files.select(Files.id).where { condition }.map { it[Files.id].value }
    }
}
