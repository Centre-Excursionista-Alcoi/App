package org.centrexcursionistalcoi.app.pdf

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.centrexcursionistalcoi.app.pdf.PdfGeneratorService.MARGIN
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.Closeable
import javax.imageio.ImageIO
import kotlin.math.roundToInt

/**
 * The document being drawn by [PdfGeneratorService], with the page being drawn and the cursor on it ([yPosition]),
 * adding pages as content needs them.
 */
internal class DrawContext(
    val document: PDDocument
): Closeable {
    lateinit var page: PDPage
        private set
    lateinit var contentStream: PDPageContentStream
        private set
    var yPosition = 0f
        private set
    val width = PDRectangle.A4.width - (2 * MARGIN)

    fun newPage() {
        if (this::contentStream.isInitialized) {
            contentStream.close()
        }
        page = PDPage(PDRectangle.A4)
        document.addPage(page)
        contentStream = PDPageContentStream(document, page)
        yPosition = page.mediaBox.height - MARGIN
    }

    init {
        newPage()
    }

    override fun close() {
        if (this::contentStream.isInitialized) {
            contentStream.close()
        }
    }

    fun moveDown(amount: Float) {
        yPosition -= amount
    }

    fun moveDown(amount: Int) = moveDown(amount.toFloat())

    fun checkPageBreak(neededHeight: Float) {
        if (yPosition - neededHeight < MARGIN) {
            newPage()
        }
    }

    fun drawText(
        text: String,
        font: PDFont,
        size: Float,
        color: Color = Color.BLACK,
        offset: Pair<Float, Float> = Pair(0f, 0f)
    ) {
        checkPageBreak(size + 2)
        contentStream.beginText()
        contentStream.setFont(font, size)
        contentStream.setNonStrokingColor(color)
        contentStream.newLineAtOffset(offset.first, offset.second)
        contentStream.showText(text)
        contentStream.endText()
        yPosition -= (size + 4)
    }

    /**
     * Draws text at the current cursor position, applying the specified font, size, and color.
     * Offsets the left side by [MARGIN].
     * @param text The text to draw.
     * @param font The PDFont to use for rendering the text.
     * @param size The font size for the text.
     * @param color The color of the text, defaulting to black.
     */
    fun drawTextAtCursor(text: String, font: PDFont, size: Float, color: Color = Color.BLACK) {
        drawText(text, font, size, color, offset = Pair(MARGIN, yPosition))
    }

    enum class ImageAlignment { Start, End }

    /**
     * Draws the image in [bytes] [height] tall, keeping its aspect ratio, with its top at the cursor, which doesn't
     * move: at the start of the line, or its end.
     * @param name The image's name in the document.
     * @return The width it was drawn with.
     */
    fun drawImageAtCursor(bytes: ByteArray, name: String, height: Float, alignment: ImageAlignment = ImageAlignment.Start): Float {
        val image = SourceImage(document, bytes, name)
        val width = image.width * height / image.height
        val left = when (alignment) {
            ImageAlignment.Start -> MARGIN
            ImageAlignment.End -> page.mediaBox.width - MARGIN - width
        }
        contentStream.drawImage(image.toXObject(width, height), left, yPosition - height, width, height)
        return width
    }

    /**
     * Draws the image in [bytes] at the cursor, as large as it fits in the page (but no larger than one point per
     * pixel), on a new page if there's no room left in this one, and moves the cursor [spacing] below it.
     * @param name The image's name in the document.
     */
    fun drawImageBlock(bytes: ByteArray, name: String, spacing: Float) {
        val image = SourceImage(document, bytes, name)
        val maxHeight = PDRectangle.A4.height - 2 * MARGIN - spacing
        val scale = minOf(1f, width / image.width, maxHeight / image.height)
        val width = image.width * scale
        val height = image.height * scale
        checkPageBreak(height + spacing)
        contentStream.drawImage(image.toXObject(width, height), MARGIN, yPosition - height, width, height)
        moveDown(height + spacing)
    }
}

/**
 * The resolution images are embedded at, for the size they're drawn at: enough to print them, without the many
 * more pixels a photo or a logo usually has, which only make the document larger.
 */
private const val IMAGE_DPI = 200f

/**
 * The image in [bytes], to embed in [document] as [name], downscaled to the size it's drawn at ([toXObject]) when
 * it's larger.
 */
private class SourceImage(private val document: PDDocument, private val bytes: ByteArray, private val name: String) {
    private val decoded: BufferedImage? = ImageIO.read(ByteArrayInputStream(bytes))

    /** The image embedded as it is, when Java can't decode it (PDFBox may still be able to). */
    private val undecoded: PDImageXObject? =
        if (decoded == null) PDImageXObject.createFromByteArray(document, bytes, name) else null

    val width: Float = (decoded?.width ?: undecoded!!.width).toFloat()
    val height: Float = (decoded?.height ?: undecoded!!.height).toFloat()

    /** The image as an object of [document], with enough pixels to draw it [width] by [height] points. */
    fun toXObject(width: Float, height: Float): PDImageXObject {
        if (decoded == null) return undecoded!!
        val targetWidth = (width * IMAGE_DPI / 72).roundToInt().coerceAtLeast(1)
        val targetHeight = (height * IMAGE_DPI / 72).roundToInt().coerceAtLeast(1)
        if (decoded.width <= targetWidth && decoded.height <= targetHeight) {
            return PDImageXObject.createFromByteArray(document, bytes, name)
        }
        val scaled = decoded.scaledTo(targetWidth, targetHeight)
        // Transparency (e.g. a logo) needs a lossless image, photos are much smaller as JPEG
        return if (scaled.colorModel.hasAlpha()) {
            LosslessFactory.createFromImage(document, scaled)
        } else {
            JPEGFactory.createFromImage(document, scaled, 0.85f)
        }
    }
}

/**
 * This image, [width] by [height] pixels. Downscaled by halves first: a single step that large would skip most of
 * the pixels, and lose details.
 */
private fun BufferedImage.scaledTo(width: Int, height: Int): BufferedImage {
    val type = if (colorModel.hasAlpha()) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB
    var current = this
    do {
        val nextWidth = maxOf(width, current.width / 2)
        val nextHeight = maxOf(height, current.height / 2)
        val next = BufferedImage(nextWidth, nextHeight, type)
        next.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            drawImage(current, 0, 0, nextWidth, nextHeight, null)
            dispose()
        }
        current = next
    } while (current.width != width || current.height != height)
    return current
}
