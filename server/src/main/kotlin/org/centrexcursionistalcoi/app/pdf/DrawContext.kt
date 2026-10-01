package org.centrexcursionistalcoi.app.pdf

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.centrexcursionistalcoi.app.pdf.PdfGeneratorService.MARGIN
import java.awt.Color
import java.io.Closeable

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
}
