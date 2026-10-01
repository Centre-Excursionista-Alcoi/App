package org.centrexcursionistalcoi.app.pdf

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDDocumentInformation
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.ReferencedMemory
import org.centrexcursionistalcoi.app.data.Sports
import org.intellij.markdown.parser.CancellationToken
import org.slf4j.LoggerFactory
import java.awt.Color
import java.io.OutputStream
import java.util.Calendar
import kotlin.uuid.Uuid

object PdfGeneratorService {
    private const val VERSION = 1
    private const val FONT_SIZE_TITLE = 18f
    private const val FONT_SIZE_HEADER = 12f
    private const val FONT_SIZE_BODY = 10f
    internal const val MARGIN = 50f

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

    private fun PDDocument.loadFontFromResources(fontName: String): PDType0Font {
        return PDType0Font.load(this, this::class.java.getResourceAsStream("/fonts/$fontName.ttf"))
    }

    fun generateLendingPdf(
        memory: ReferencedMemory,
        itemsUsed: List<ReferencedInventoryItem>,
        submittedBy: String,
        photoProvider: (Uuid) -> ByteArray, // Callback to fetch actual image data
        outputStream: OutputStream,
        cancellationToken: CancellationToken = CancellationToken.NonCancellable
    ) {
        PDDocument().use { document ->
            document.documentInformation = PDDocumentInformation().apply {
                title = "Memòria d'Activitat"
                author = submittedBy
                creator = "Centre Excursionista Alcoi"
                subject = "Memòria d'Activitat"
                keywords = "PDF, Memòria, Activitat, CEA"
                creationDate = Calendar.getInstance()

                setCustomMetadataValue("version", VERSION.toString())
                setCustomMetadataValue("memoryId", memory.id.toString())
                setCustomMetadataValue("lendingId", memory.lending?.toString())
            }

            // --- State Management ---
            val context = DrawContext(document)

            fun contentStream(): PDPageContentStream = context.contentStream
            fun yPosition(): Float = context.yPosition

            // Load Fonts
            val fontRegular = document.loadFontFromResources("RobotoCondensed-Light")
            val fontTitles = document.loadFontFromResources("Nunito-Bold")
            val fontErrors = fontTitles

            // =========================================
            // 1. Header & Logo
            // =========================================
            try {
                val logo = this::class.java.getResourceAsStream("/cea.png")!!.readBytes()
                val logoImage = PDImageXObject.createFromByteArray(document, logo, "Logo CEA")
                val scale = 50f / logoImage.height // Scale to 50px height
                val logoWidth = logoImage.width * scale

                contentStream().drawImage(logoImage, MARGIN, yPosition() - 50, logoWidth, 50f)

                // Draw Title next to Logo
                context.drawText("Memòria d'Activitat", fontTitles, FONT_SIZE_TITLE, offset = Pair(MARGIN + logoWidth + 10, yPosition() - 30))

                // If a department is given, and it has an image, draw it on the right hand side
                val department = memory.department ?: itemsUsed.firstNotNullOfOrNull { it.type.department }
                if (department?.image != null) {
                    val deptImageBytes = photoProvider(department.image!!)
                    val deptImage = PDImageXObject.createFromByteArray(document, deptImageBytes, department.displayName)
                    val deptScale = 50f / deptImage.height
                    val deptWidth = deptImage.width * deptScale

                    contentStream().drawImage(deptImage, context.page.mediaBox.width - MARGIN - deptWidth, yPosition() - 50, deptWidth, 50f)
                }

                context.moveDown(70) // Space after header
            } catch (e: Exception) {
                context.drawTextAtCursor("[Logo Error]", fontErrors, FONT_SIZE_BODY, Color.RED)
                logger.error("Error loading logo image for PDF", e)
            }

            // =========================================
            // 2. Metadata (Submitted By, Date, Place)
            // =========================================
            context.drawTextAtCursor("Enviada per: $submittedBy", fontTitles, FONT_SIZE_HEADER)

            val fromDate = memory.from.toStringCompact()
            val toDate = memory.to.toStringCompact()
            context.drawTextAtCursor("Dates: des del $fromDate fins al $toDate", fontRegular, FONT_SIZE_BODY)

            if (memory.place != null) {
                context.drawTextAtCursor("Lloc: ${memory.place}", fontRegular, FONT_SIZE_BODY)
            }

            if (memory.sport != null) {
                context.drawTextAtCursor("Esport: ${memory.sport?.displayName()}", fontRegular, FONT_SIZE_BODY)
            }

            context.moveDown(10) // Spacer

            // =========================================
            // 3. Participants
            // =========================================
            if (memory.members.isNotEmpty()) {
                context.drawTextAtCursor("Socis:", fontTitles, FONT_SIZE_HEADER)
                memory.members.forEach { member ->
                    context.drawTextAtCursor("- ${member.fullName}", fontRegular, FONT_SIZE_BODY)
                }
                context.moveDown(10)
            }

            if (!memory.externalUsers.isNullOrEmpty()) {
                context.drawTextAtCursor("Altres participants:", fontTitles, FONT_SIZE_HEADER)
                val externalUsers = memory.externalUsers
                if (!externalUsers.isNullOrBlank()) {
                    externalUsers.split("\n").forEach { user ->
                        context.drawTextAtCursor("- $user", fontRegular, FONT_SIZE_BODY)
                    }
                }
                context.moveDown(10)
            }

            // =========================================
            // 4. Items Used
            // =========================================
            if (itemsUsed.isNotEmpty()) {
                context.drawTextAtCursor("Material del club utilitzat:", fontTitles, FONT_SIZE_HEADER)
                itemsUsed.groupBy { item -> item.type }.forEach { (type, items) ->
                    context.drawTextAtCursor("- x${items.size} ${type.displayName}", fontRegular, FONT_SIZE_BODY)
                }
                context.moveDown(10)
            }

            // =========================================
            // 5. Markdown Text (Long Text)
            // =========================================
            context.drawTextAtCursor("Descripció de l'activitat:", fontTitles, FONT_SIZE_HEADER)

            MarkdownPdfRenderer(context).draw(
                markdownText = memory.text,
                font = fontRegular,
                size = FONT_SIZE_BODY,
                color = Color.BLACK,
                headingFont = fontTitles,
                cancellationToken = cancellationToken
            )

            // =========================================
            // 6. Photos
            // =========================================
            if (memory.attachments.isNotEmpty()) {
                context.drawTextAtCursor("Fotos:", fontTitles, FONT_SIZE_HEADER)

                memory.attachments.forEach { uuid ->
                    try {
                        val photo = photoProvider(uuid)
                        val pdImage = PDImageXObject.createFromByteArray(document, photo, uuid.toString())

                        // Logic to fit image within page width
                        var imgWidth = pdImage.width.toFloat()
                        var imgHeight = pdImage.height.toFloat()

                        val maxWidth = context.width
                        if (imgWidth > maxWidth) {
                            val scale = maxWidth / imgWidth
                            imgWidth = maxWidth
                            imgHeight *= scale
                        }

                        // Check space, if not enough, new page
                        context.checkPageBreak(imgHeight + 20)

                        context.contentStream.drawImage(pdImage, MARGIN, context.yPosition - imgHeight, imgWidth, imgHeight)
                        context.moveDown(imgHeight + 20)
                    } catch (e: Exception) {
                        context.drawTextAtCursor("Error loading image: $uuid", fontRegular, FONT_SIZE_BODY, Color.RED)
                        logger.error("Error loading image for PDF: $uuid", e)
                    }
                }
            }

            // Close the final content stream before adding footers
            context.close()

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
