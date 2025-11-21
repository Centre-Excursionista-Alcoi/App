package org.centrexcursionistalcoi.app.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import org.centrexcursionistalcoi.app.ResourcesUtils

class TestFileTypeUtils {
    @Test
    fun test_detectFileType_jpg() {
        val bytes = ResourcesUtils.bytesFromResource("/image.jpg")
        val detectedType = detectFileType(bytes)
        assertEquals(FileType.JPEG, detectedType)
    }

    @Test
    fun test_detectFileType_png() {
        val bytes = ResourcesUtils.bytesFromResource("/image.png")
        val detectedType = detectFileType(bytes)
        assertEquals(FileType.PNG, detectedType)
    }

    @Test
    fun test_detectFileType_pdf() {
        val bytes = ResourcesUtils.bytesFromResource("/document.pdf")
        val detectedType = detectFileType(bytes)
        assertEquals(FileType.PDF, detectedType)
    }
}
