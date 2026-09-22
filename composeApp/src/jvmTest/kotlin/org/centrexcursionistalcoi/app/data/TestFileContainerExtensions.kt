package org.centrexcursionistalcoi.app.data

import kotlinx.io.files.SystemPathSeparator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

private class FakeFileContainer(override val files: Map<String, Uuid?>) : FileContainer

/**
 * Every current [FileContainer] is a PDF document (insurance/memory attachments) -- [FileContainer.filePaths]
 * stores it with that extension already, so it can be shared/opened as-is with no rename or copy (#673). This
 * would have caught the double-extension footgun a naive "just append .pdf" fix could introduce if the cached
 * path itself were ever built from something already carrying one.
 */
class TestFileContainerExtensions {
    private val uuid = Uuid.parse("1f0e5c2a-0000-4000-8000-000000000001")
    private val sep = SystemPathSeparator

    @Test
    fun filePaths_storesEveryFile_withThePdfExtension() {
        val container = FakeFileContainer(mapOf("documentId" to uuid))

        val paths = container.filePaths()

        assertEquals(mapOf(uuid to "files$sep${FakeFileContainer::class.simpleName}$sep$uuid.pdf"), paths)
    }

    @Test
    fun filePaths_skipsEntriesWithNoFile() {
        val container = FakeFileContainer(mapOf("documentId" to null))

        assertEquals(emptyMap(), container.filePaths())
    }

    @Test
    fun filePaths_coversEveryNonNullEntry_keyedByItsOwnUuid() {
        val otherUuid = Uuid.parse("1f0e5c2a-0000-4000-8000-000000000002")
        val container = FakeFileContainer(mapOf("a" to uuid, "b" to otherUuid, "c" to null))

        val paths = container.filePaths()

        assertEquals(setOf(uuid, otherUuid), paths.keys)
        assertEquals(true, paths.getValue(uuid).endsWith("$uuid.pdf"))
        assertEquals(true, paths.getValue(otherUuid).endsWith("$otherUuid.pdf"))
    }
}
