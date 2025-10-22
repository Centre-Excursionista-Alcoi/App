package org.centrexcursionistalcoi.app.storage.fs

import io.ktor.utils.io.ByteReadChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class TestPlatformFileSystem {
    @Test
    fun testWriteRead() = runTest {
        val channel = ByteReadChannel("Hello, World!")
        PlatformFileSystem.write("test.txt", channel)
        val data = PlatformFileSystem.read("test.txt")
        assertEquals(data.decodeToString(), "Hello, World!")
    }

    @Test
    fun testWriteRead_subDirectory() = runTest {
        val channel = ByteReadChannel("Hello, World!")
        PlatformFileSystem.write("example/test.txt", channel)
        val data = PlatformFileSystem.read("example/test.txt")
        assertEquals(data.decodeToString(), "Hello, World!")
    }
}
