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
}
