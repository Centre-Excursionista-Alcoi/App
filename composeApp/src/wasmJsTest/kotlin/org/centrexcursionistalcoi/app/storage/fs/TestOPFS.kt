package org.centrexcursionistalcoi.app.storage.fs

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalWasmJsInterop::class)
class TestOPFS {
    @BeforeTest
    fun prepareNapier() {
        Napier.base(DebugAntilog())
    }

    @Test
    fun testWriteAndReadText() = runTest {
        val root = OPFS.root()
        try {
            OPFS.writeTextFile(root, "test.txt", "This is a test file")

            val content = OPFS.readTextFile(root, "test.txt")
            assertEquals("This is a test file", content)
        } finally {
            root.removeEntry("test.txt")
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun testWriteAndReadBytes() = runTest {
        val data = ByteArray(256) { it.toByte() }
        val root = OPFS.root()
        try {
            OPFS.writeFile(root, "test.txt", data)

            val content = OPFS.readFile(root, "test.txt")
            assertContentEquals(data, content)
        } finally {
            root.removeEntry("test.txt")
        }
    }

    @Test
    fun testClear() = runTest {
        val root = OPFS.root()
        OPFS.writeTextFile(root, "test.txt", "This is a test file")

        OPFS.clear()

        assertNull(OPFS.readFile(root, "test.txt"))
    }
}
