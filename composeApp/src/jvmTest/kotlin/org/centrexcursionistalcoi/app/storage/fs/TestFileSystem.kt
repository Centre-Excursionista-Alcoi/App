package org.centrexcursionistalcoi.app.storage.fs

import io.github.vinceglb.filekit.utils.toKotlinxIoPath
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestFileSystem {
    @Test
    fun test_deleteRecursively() {
        val dir = createTempDirectory().toFile()
        dir.deleteOnExit()
        File(dir, "file").createNewFile()
        val subdir = File(dir, "subdir")
        subdir.mkdir()
        File(subdir, "file").createNewFile()

        val deletedCount = FileSystem.deleteRecursively(dir.toKotlinxIoPath())
        assertEquals(4, deletedCount)
        assertTrue(!dir.exists())
    }

    /**
     * A plain deletion failure (kotlinx-io's SystemFileSystem throws a bare `IOException("Deletion failed")`
     * whenever `File.delete()` returns `false` for an existing path -- e.g. a directory a concurrent write raced
     * back to non-empty) must not abort the whole recursive delete, only skip that one entry. Reproduced here via
     * a read-only parent directory, which makes `File.delete()` on its child fail the same way a concurrent write
     * would.
     */
    @Test
    fun test_deleteRecursively_toleratesDeletionFailure() {
        val dir = createTempDirectory().toFile()
        dir.deleteOnExit()
        val child = File(dir, "child").apply { createNewFile() }

        dir.setWritable(false)
        try {
            val deletedCount = FileSystem.deleteRecursively(dir.toKotlinxIoPath())
            assertEquals(0, deletedCount)
            assertTrue(dir.exists())
            assertTrue(child.exists())
        } finally {
            dir.setWritable(true)
        }
    }
}
