package org.centrexcursionistalcoi.app.pdf

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.ReferencedLendingMemory
import org.centrexcursionistalcoi.app.data.Sports
import org.slf4j.LoggerFactory
import java.awt.Color
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.uuid.toJavaUuid

object PdfGeneratorService {
    private val fontRegular: PDFont = PDType1Font.HELVETICA
    private val fontBold: PDFont = PDType1Font.HELVETICA_BOLD
    private const val FONT_SIZE_TITLE = 18f
    private const val FONT_SIZE_HEADER = 12f
    private const val FONT_SIZE_BODY = 10f
    private const val MARGIN = 50f

    private val logger = LoggerFactory.getLogger(this::class.java)

    private fun Sports.displayName(): String = when (this) {
        Sports.CLIMBING -> "Escalada"
        Sports.CLIMBING_WITHOUT_BELAY -> "Escalada sense assegurança"
        Sports.VIA_FERRATA -> "Vies ferrades"
        Sports.CANYONING -> "Barranquisme"
        Sports.HIKING -> "Senderisme"
        Sports.ALPINISM -> "Alpinisme"
        Sports.ORIENTEERING -> "Orientació"
        Sports.NORDIC_WALKING -> "Marxa nòrdica"
        Sports.SPELEOLOGY -> "Espeleologia"
        Sports.CYCLING -> "Ciclisme"
        Sports.CULTURAL_TOURISM -> "Turisme cultural"
    }

    fun generateLendingPdf(
        memory: ReferencedLendingMemory,
        itemsUsed: List<ReferencedInventoryItem>,
        submittedBy: String,
        dateRange: Pair<LocalDate, LocalDate>,
        photoProvider: (UUID) -> ByteArray, // Callback to fetch actual image data
        outputStream: OutputStream
    ) {
        PDDocument().use { document ->
            // --- State Management ---
            var page = PDPage(PDRectangle.A4)
            document.addPage(page)
            var contentStream = PDPageContentStream(document, page)

            // Start Y position (Top of page, moving down)
            var yPosition = page.mediaBox.height - MARGIN
            val width = page.mediaBox.width - (2 * MARGIN)

            // --- Helper: Page Break Logic ---
            fun checkPageBreak(neededHeight: Float) {
                if (yPosition - neededHeight < MARGIN) {
                    contentStream.close() // Close current
                    page = PDPage(PDRectangle.A4)
                    document.addPage(page)
                    contentStream = PDPageContentStream(document, page)
                    yPosition = page.mediaBox.height - MARGIN
                }
            }

            // --- Helper: Text Drawer ---
            fun drawText(text: String, font: PDFont, size: Float, color: Color = Color.BLACK) {
                checkPageBreak(size + 2)
                contentStream.beginText()
                contentStream.setFont(font, size)
                contentStream.setNonStrokingColor(color)
                contentStream.newLineAtOffset(MARGIN, yPosition)
                contentStream.showText(text)
                contentStream.endText()
                yPosition -= (size + 4) // Move cursor down
            }

            // --- Helper: Multi-line Text Wrapper ---
            fun drawWrappedText(text: String, font: PDFont = fontRegular) {
                val words = text.split(" ")
                var line = ""

                for (word in words) {
                    val testLine = if (line.isEmpty()) word else "$line $word"
                    val textSize = font.getStringWidth(testLine) / 1000 * FONT_SIZE_BODY

                    if (textSize > width) {
                        drawText(line, font, FONT_SIZE_BODY)
                        line = word
                    } else {
                        line = testLine
                    }
                }
                if (line.isNotEmpty()) {
                    drawText(line, font, FONT_SIZE_BODY)
                }
            }

            // =========================================
            // 1. Header & Logo
            // =========================================
            try {
                val logo = this::class.java.getResourceAsStream("/cea.png")!!.readBytes()
                val logoImage = PDImageXObject.createFromByteArray(document, logo, "Logo CEA")
                val scale = 50f / logoImage.height // Scale to 50px height
                val logoWidth = logoImage.width * scale

                contentStream.drawImage(logoImage, MARGIN, yPosition - 50, logoWidth, 50f)

                // Draw Title next to Logo
                contentStream.beginText()
                contentStream.setFont(fontBold, FONT_SIZE_TITLE)
                contentStream.newLineAtOffset(MARGIN + logoWidth + 10, yPosition - 30)
                contentStream.showText("Memòria d'Activitat")
                contentStream.endText()

                // If a department is given, and it has an image, draw it on the right hand side
                val department = memory.department ?: itemsUsed.firstNotNullOfOrNull { it.type.department }
                if (department?.image != null) {
                    val deptImageBytes = photoProvider(department.image!!.toJavaUuid())
                    val deptImage = PDImageXObject.createFromByteArray(document, deptImageBytes, department.displayName)
                    val deptScale = 50f / deptImage.height
                    val deptWidth = deptImage.width * deptScale

                    contentStream.drawImage(deptImage, page.mediaBox.width - MARGIN - deptWidth, yPosition - 50, deptWidth, 50f)
                }

                yPosition -= 70 // Space after header
            } catch (e: Exception) {
                drawText("[Logo Error]", fontBold, FONT_SIZE_BODY, Color.RED)
                logger.error("Error loading logo image for PDF", e)
            }

            // =========================================
            // 2. Metadata (Submitted By, Date, Place)
            // =========================================
            drawText("Enviada per: $submittedBy", fontBold, FONT_SIZE_HEADER)

            val fmt = DateTimeFormatter.ofPattern("dd MMMM, yyyy")
            drawText("Dates: des del ${dateRange.first.format(fmt)} fins al ${dateRange.second.format(fmt)}", fontRegular, FONT_SIZE_BODY)

            if (memory.place != null) {
                drawText("Lloc: ${memory.place}", fontRegular, FONT_SIZE_BODY)
            }

            if (memory.sport != null) {
                drawText("Esport: ${memory.sport?.displayName()}", fontRegular, FONT_SIZE_BODY)
            }

            yPosition -= 10 // Spacer

            // =========================================
            // 3. Participants
            // =========================================
            drawText("Socis:", fontBold, FONT_SIZE_HEADER)
            memory.members.forEach { member ->
                drawText("- ${member.fullName}", fontRegular, FONT_SIZE_BODY)
            }
            yPosition -= 10

            drawText("Altres participants:", fontBold, FONT_SIZE_HEADER)
            val externalUsers = memory.externalUsers
            if (!externalUsers.isNullOrBlank()) {
                externalUsers.split("\n").forEach { user ->
                    drawText("- $user", fontRegular, FONT_SIZE_BODY)
                }
            }
            yPosition -= 10

            // =========================================
            // 4. Items Used
            // =========================================
            drawText("Material del club utilitzat:", fontBold, FONT_SIZE_HEADER)
            itemsUsed.groupBy { item -> item.type }.forEach { (type, items) ->
                drawText("- x${items.size} ${type.displayName}", fontRegular, FONT_SIZE_BODY)
            }
            yPosition -= 10

            // =========================================
            // 5. Markdown Text (Long Text)
            // =========================================
            drawText("Descripció de l'activitat:", fontBold, FONT_SIZE_HEADER)

            // Simple Markdown Clean-up (PDFBox doesn't support bolding inside strings natively)
            // We split by newlines to preserve paragraphs
            val paragraphs = memory.text.split("\n")

            paragraphs.forEach { paragraph ->
                if (paragraph.isNotBlank()) {
                    // Remove markdown headers logic for cleaner display
                    val cleanText = paragraph.replace("#", "").trim()
                    drawWrappedText(cleanText)
                    yPosition -= 5 // small gap between paragraphs
                }
            }
            yPosition -= 20

            // =========================================
            // 6. Photos
            // =========================================
            if (memory.files.isNotEmpty()) {
                drawText("Fotos:", fontBold, FONT_SIZE_HEADER)

                memory.files.forEach { uuid ->
                    try {
                        val photo = photoProvider(uuid.toJavaUuid())
                        val pdImage = PDImageXObject.createFromByteArray(document, photo, uuid.toString())

                        // Logic to fit image within page width
                        var imgWidth = pdImage.width.toFloat()
                        var imgHeight = pdImage.height.toFloat()

                        val maxWidth = width
                        if (imgWidth > maxWidth) {
                            val scale = maxWidth / imgWidth
                            imgWidth = maxWidth
                            imgHeight *= scale
                        }

                        // Check space, if not enough, new page
                        checkPageBreak(imgHeight + 20)

                        contentStream.drawImage(pdImage, MARGIN, yPosition - imgHeight, imgWidth, imgHeight)
                        yPosition -= (imgHeight + 20)
                    } catch (e: Exception) {
                        drawText("Error loading image: $uuid", fontRegular, FONT_SIZE_BODY, Color.RED)
                        logger.error("Error loading image for PDF: $uuid", e)
                    }
                }
            }

            // Close the final content stream before adding footers
            contentStream.close()

            // =========================================
            // 7. Footer (Page Numbers)
            // =========================================
            val totalPages = document.numberOfPages
            for (i in 0 until totalPages) {
                val footerPage = document.getPage(i)
                val footerStream = PDPageContentStream(document, footerPage, PDPageContentStream.AppendMode.APPEND, true, true)

                footerStream.beginText()
                footerStream.setFont(fontRegular, 10f)
                // Center the page number
                val pageText = "Pàgina ${i + 1} de $totalPages"
                val textSize = fontRegular.getStringWidth(pageText) / 1000 * 10f
                val centerX = (footerPage.mediaBox.width - textSize) / 2

                footerStream.newLineAtOffset(centerX, 20f) // 20 units from bottom
                footerStream.showText(pageText)
                footerStream.endText()
                footerStream.close()
            }

            document.save(outputStream)
        }
    }
}
