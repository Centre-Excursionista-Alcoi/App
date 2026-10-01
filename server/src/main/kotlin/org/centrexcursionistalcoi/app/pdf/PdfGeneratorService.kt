package org.centrexcursionistalcoi.app.pdf

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDDocumentInformation
import org.apache.pdfbox.pdmodel.PDPageContentStream
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

            fun yPosition(): Float = context.yPosition

            // Load Fonts
            val fonts = PdfFonts(document)
            val fontRegular = fonts.body.regular
            val fontTitles = fonts.titles.regular
            val fontErrors = fontTitles

            // =========================================
            // 1. Header & Logo
            // =========================================
            try {
                val logo = this::class.java.getResourceAsStream("/cea.png")!!.readBytes()
                val logoWidth = context.drawImageAtCursor(logo, "Logo CEA", height = 50f)

                // If a department is given, and it has an image, draw it on the right hand side. Before the title,
                // which moves the cursor down.
                val department = memory.department ?: itemsUsed.firstNotNullOfOrNull { it.type.department }
                if (department?.image != null) {
                    context.drawImageAtCursor(
                        photoProvider(department.image!!),
                        department.displayName,
                        height = 50f,
                        alignment = DrawContext.ImageAlignment.End,
                    )
                }

                // Draw Title next to Logo
                context.drawText("Memòria d'Activitat", fontTitles, FONT_SIZE_TITLE, offset = Pair(MARGIN + logoWidth + 10, yPosition() - 30))

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
                font = fonts.body,
                size = FONT_SIZE_BODY,
                color = Color.BLACK,
                headingFont = fonts.titles,
                codeFont = fonts.code,
                cancellationToken = cancellationToken
            )

            // =========================================
            // 6. Photos
            // =========================================
            if (memory.attachments.isNotEmpty()) {
                context.drawTextAtCursor("Fotos:", fontTitles, FONT_SIZE_HEADER)

                memory.attachments.forEach { uuid ->
                    try {
                        context.drawImageBlock(photoProvider(uuid), uuid.toString(), spacing = 20f)
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
