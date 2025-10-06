package org.centrexcursionistalcoi.app.storage.fs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class TestPlatformFileSystem {
    @Test
    fun testWriteRead() = runTest {
        PlatformFileSystem.write("test.txt", "Hello, World!".encodeToByteArray())
        val data = PlatformFileSystem.read("test.txt")
        assertEquals(data.decodeToString(), "Hello, World!")
    }

    @Test
    fun testWriteRead_subDirectory() = runTest {
        PlatformFileSystem.write("example/test.txt", "Hello, World!".encodeToByteArray())
        val data = PlatformFileSystem.read("example/test.txt")
        assertEquals(data.decodeToString(), "Hello, World!")
    }
}
