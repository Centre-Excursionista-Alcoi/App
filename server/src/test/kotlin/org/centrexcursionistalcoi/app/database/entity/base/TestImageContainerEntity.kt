package org.centrexcursionistalcoi.app.database.entity.base

import io.ktor.http.ContentType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.centrexcursionistalcoi.app.ResourcesUtils
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class TestImageContainerEntity {
    @Test
    fun test_updateOrSetImage() {
        try {
            val imageBytes = ResourcesUtils.bytesFromResource("/square.png")

            val transaction = mockk<JdbcTransaction>()
            val fileEntity = mockk<FileEntity>().apply {
                every { name } returns "square.png"
                every { type } returns ContentType.Image.PNG.toString()
                every { data } returns imageBytes
            }
            mockkObject(FileEntity)
            every { FileEntity.new(any(), any()) } returns fileEntity

            val imageContainerEntity = object : ImageContainerEntity {
                override var image: FileEntity? = null
            }
            with(transaction) {
                imageContainerEntity.updateOrSetImage(
                    imageBytes, "square.png", ContentType.Image.PNG
                )
            }
            val image = imageContainerEntity.image
            assertNotNull(image, "Image should not be null after setting it")
            assertEquals("square.png", image.name, "Image name should match")
            assertEquals(ContentType.Image.PNG.toString(), image.type, "Image content type should match")
            assertContentEquals(
                imageBytes,
                image.data,
                "Image data should match the provided bytes"
            )

            verify { FileEntity.new(any(), any()) }
        } finally {
            unmockkObject(FileEntity)
        }
    }
}
