package org.centrexcursionistalcoi.app.pdf

import java.awt.Color
import java.awt.GradientPaint
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.datetime.TimeZone
import org.apache.pdfbox.Loader
import org.apache.pdfbox.contentstream.PDFStreamEngine
import org.apache.pdfbox.contentstream.operator.Operator
import org.apache.pdfbox.contentstream.operator.state.Concatenate
import org.apache.pdfbox.contentstream.operator.state.Restore
import org.apache.pdfbox.contentstream.operator.state.Save
import org.apache.pdfbox.contentstream.operator.state.SetGraphicsStateParameters
import org.apache.pdfbox.contentstream.operator.state.SetMatrix
import org.apache.pdfbox.cos.COSBase
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.text.PDFTextStripper
import org.centrexcursionistalcoi.app.data.Memory
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.utils.toUuid

/**
 * The images drawn by [PdfGeneratorService.generateLendingPdf]: downscaled to the size they're drawn at.
 */
class TestPdfImages {
    /** An image as drawn: its pixels, and the size it's drawn at, in points. */
    private class DrawnImage(val pixelWidth: Int, val pixelHeight: Int, val width: Float, val height: Float, val bottom: Float)

    private class Pdf(val bytes: Int, val text: String, val images: List<DrawnImage>)

    /** Finds where images are drawn ("Do" operators), with the transformation they're drawn with. */
    private class ImageLocations : PDFStreamEngine() {
        val images = mutableListOf<DrawnImage>()

        init {
            addOperator(Concatenate(this))
            addOperator(SetGraphicsStateParameters(this))
            addOperator(Save(this))
            addOperator(Restore(this))
            addOperator(SetMatrix(this))
        }

        override fun processOperator(operator: Operator, operands: List<COSBase>) {
            if (operator.name == "Do") {
                val image = resources.getXObject(operands[0] as COSName)
                if (image is PDImageXObject) {
                    val matrix = graphicsState.currentTransformationMatrix
                    images += DrawnImage(image.width, image.height, matrix.scalingFactorX, matrix.scalingFactorY, matrix.translateY)
                }
            } else {
                super.processOperator(operator, operands)
            }
        }
    }

    private fun pdf(photos: List<ByteArray> = emptyList()): Pdf {
        val now = ZonedDateTime.fromInstant(Clock.System.now(), TimeZone.currentSystemDefault())
        val ids = photos.map { Uuid.random() }
        val memory = Memory(
            id = "9b57a238-6a3a-4a1a-9f4a-6f4b1e6f5a11".toUuid(),
            place = null,
            members = emptyList(),
            externalUsers = null,
            text = "Text",
            sport = null,
            department = null,
            attachments = ids,
            submittedBy = FakeUser.SUB,
            from = now,
            to = now,
            pdf = null,
            lending = null,
        )
        val bytes = ByteArrayOutputStream().use { output ->
            PdfGeneratorService.generateLendingPdf(
                memory = memory.referenced(listOf(FakeUser.data()), emptyList(), emptyList()),
                itemsUsed = emptyList(),
                submittedBy = "Admin User",
                photoProvider = { id -> photos[ids.indexOf(id)] },
                outputStream = output,
            )
            output.toByteArray()
        }
        return Loader.loadPDF(bytes).use { document ->
            val locations = ImageLocations()
            for (page in document.pages) locations.processPage(page)
            Pdf(bytes.size, PDFTextStripper().getText(document), locations.images)
        }
    }

    private fun image(width: Int, height: Int, format: String): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        image.createGraphics().apply {
            paint = GradientPaint(0f, 0f, Color.BLUE, width.toFloat(), height.toFloat(), Color.ORANGE)
            fillRect(0, 0, width, height)
            dispose()
        }
        return ByteArrayOutputStream().use { ImageIO.write(image, format, it); it.toByteArray() }
    }

    /** The pixels needed to draw [points] at the 200 DPI images are embedded at, with some rounding margin. */
    private fun pixelsFor(points: Float) = (points * 200 / 72).toInt() + 1

    @Test
    fun test_logo_downscaled() {
        val pdf = pdf()

        val logo = pdf.images.single()
        assertEquals(50f, logo.height, 0.01f)
        assertTrue(logo.pixelHeight <= pixelsFor(50f), "Logo embedded at ${logo.pixelWidth}x${logo.pixelHeight}")
        // The logo alone used to make it about 600 KB
        assertTrue(pdf.bytes < 100_000, "PDF is ${pdf.bytes} bytes")
    }

    @Test
    fun test_largePhoto_downscaledToPageWidth() {
        val pageWidth = PDRectangle.A4.width - 2 * PdfGeneratorService.MARGIN
        val photo = pdf(listOf(image(4000, 3000, "jpg"))).images.last()

        assertEquals(pageWidth, photo.width, 0.01f)
        assertTrue(photo.pixelWidth <= pixelsFor(pageWidth), "Photo embedded at ${photo.pixelWidth}x${photo.pixelHeight}")
        assertEquals(4f / 3, photo.pixelWidth.toFloat() / photo.pixelHeight, 0.01f)
    }

    @Test
    fun test_tallPhoto_fitsInPage() {
        val photo = pdf(listOf(image(1000, 4000, "jpg"))).images.last()

        assertTrue(photo.bottom >= PdfGeneratorService.MARGIN, "Photo drawn below the margin, at ${photo.bottom}")
        assertTrue(photo.height <= PDRectangle.A4.height - 2 * PdfGeneratorService.MARGIN)
        assertTrue(photo.pixelHeight <= pixelsFor(photo.height), "Photo embedded at ${photo.pixelWidth}x${photo.pixelHeight}")
    }

    @Test
    fun test_smallImage_notUpscaled() {
        val photo = pdf(listOf(image(100, 80, "png"))).images.last()

        assertEquals(100, photo.pixelWidth)
        assertEquals(80, photo.pixelHeight)
        assertEquals(100f, photo.width, 0.01f)
    }

    @Test
    fun test_notAnImage_showsAnError() {
        val pdf = pdf(listOf("not an image".encodeToByteArray()))

        assertContains(pdf.text, "Error loading image")
    }
}
