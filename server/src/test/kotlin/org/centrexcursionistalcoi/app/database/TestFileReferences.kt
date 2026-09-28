package org.centrexcursionistalcoi.app.database

import kotlin.test.Test
import kotlin.test.assertEquals
import org.centrexcursionistalcoi.app.database.table.Files

class TestFileReferences {
    @Test
    fun test_everyReferenceToFilesIsListed() {
        // Files referenced by a column missing from FileReferences would be deleted while still in use
        val references = Database.tables
            .flatMap { it.columns }
            .filter { it.referee?.table == Files }
            .map { "${it.table.tableName}.${it.name}" }
            .toSet()
        val listed = FileReferences.columns.map { "${it.table.tableName}.${it.name}" }.toSet()

        assertEquals(references, listed)
    }
}
