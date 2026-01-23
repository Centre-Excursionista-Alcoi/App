package org.centrexcursionistalcoi.app.fs

import io.ktor.http.ContentType
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.fs.VirtualFileSystem.resetRootDirs
import org.centrexcursionistalcoi.app.utils.toUUID
import org.jetbrains.exposed.v1.dao.Entity

class TestVirtualFileSystem {
    @BeforeTest
    fun setUp() {
        Database.initForTests()

        val file = Database {
            FileEntity.new("6489d244-cd88-4441-9526-1a4627b67453".toUUID()) {
                name = "example.txt"
                type = "text/plain"
                bytes = byteArrayOf(10, 20, 30)
                lastModified = Instant.ofEpochSecond(1763383432)
            }
        }

        val mockEntity = mockk<Entity<Any>>()
        every { mockEntity.id.value } returns 123

        val mockDir = mockk<VirtualFileSystem.RootDir<Any, Entity<Any>>>()
        every { mockDir.name } returns "MockDir"
        every { mockDir.all() } answers { listOf(mockEntity to file) }
        every { mockDir.findByStringIdOrNull(any()) } returns file
        every { mockDir.fileDisplayName(any(), any()) } returns "example.txt"

        VirtualFileSystem.rootDirs = listOf(mockDir)
    }

    @AfterTest
    fun tearDown() {
        Database.clear()
        resetRootDirs()
    }

    @Test
    fun test_list() {
        // Listing root dirs returns the directories
        val root = VirtualFileSystem.list("/")
        assertNotNull(root)
        assertEquals(1, root.size)
        root[0].let { item ->
            assertEquals("MockDir", item.name)
            assertEquals("/webdav/MockDir/", item.path)
            assertEquals(true, item.isDirectory)
        }

        // Listing a directory returns its children
        val mockDirList = VirtualFileSystem.list("/MockDir")
        assertNotNull(mockDirList)
        assertEquals(1, mockDirList.size)
        mockDirList[0].let { item ->
            assertEquals("example.txt", item.name)
            assertEquals("/webdav/MockDir/123", item.path)
            assertEquals(false, item.isDirectory)
            assertEquals(3, item.size)
            assertEquals(Instant.ofEpochSecond(1763383432), item.lastModified)
        }

        // Files cannot be listed
        val fileDirList = VirtualFileSystem.list("/MockDir/123")
        assertNull(fileDirList)
    }

    @Test
    fun test_read() {
        // Directories cannot be read
        assertNull(
            VirtualFileSystem.read("/")
        )
        assertNull(
            VirtualFileSystem.read("/MockDir")
        )

        // Reading a file returns its data
        val itemData = VirtualFileSystem.read("/MockDir/6489d244-cd88-4441-9526-1a4627b67453")
        assertNotNull(itemData)
        assertEquals(ContentType.Text.Plain, itemData.contentType)
        assertEquals(3, itemData.size)
        assertContentEquals(byteArrayOf(10, 20, 30), itemData.data)
    }
}
