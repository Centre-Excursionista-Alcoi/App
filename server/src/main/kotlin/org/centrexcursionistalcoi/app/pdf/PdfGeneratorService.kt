package org.centrexcursionistalcoi.app.pdf

import io.ktor.http.ContentType
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDDocumentInformation
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem.Companion.referenced
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType.Companion.referenced
import org.centrexcursionistalcoi.app.data.ReferencedMemory
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.entity.MemoryEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.pdf.PdfGeneratorService.VERSION
import org.centrexcursionistalcoi.app.security.FileReadWriteRules
import org.centrexcursionistalcoi.app.storage.FileStorageProvider
import org.centrexcursionistalcoi.app.verification.DocumentType
import org.centrexcursionistalcoi.app.verification.DocumentVerification
import org.intellij.markdown.parser.CancellationToken
import org.slf4j.LoggerFactory
import java.awt.Color
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.Calendar
import java.util.Date
import kotlin.uuid.Uuid

object PdfGeneratorService {
    /** Increase when the PDFs change, so existing ones are generated again (see [updateMemoriesIfNeeded]). */
    private const val VERSION = 1
    private const val FONT_SIZE_TITLE = 18f
    private const val FONT_SIZE_HEADER = 12f
    private const val FONT_SIZE_BODY = 10f
    internal const val MARGIN = 50f

    private const val META_VERSION = "version"
    private const val META_MEMORY_ID = "memoryId"
    private const val META_LENDING_ID = "lendingId"

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

    /**
     * Checks if the PDF for the given file needs to be updated.
     * This is determined by checking the version metadata in the PDF's document information.
     *
     * A PDF needs an update if:
     * 1. The version metadata is missing.
     * 2. The version metadata is less than the current [VERSION].
     */
    fun needsUpdate(file: FileEntity): Boolean {
        val bytes = FileStorageProvider.current.open(file.objectKey).use { stream -> stream.readAllBytes() }
        return Loader.loadPDF(bytes).use { document ->
            val information = document.documentInformation

            val version = information.getCustomMetadataValue(META_VERSION)?.toIntOrNull()
            version == null || version < VERSION
        }
    }

    /**
     * Updates all memories in the database that need a PDF update.
     * A memory needs a PDF update if it has no PDF, or if the existing PDF is outdated (as determined by [needsUpdate]).
     * This function will generate a new PDF for each memory that needs an update, and save it to the database.
     * If a memory already has a PDF, it will be replaced with the new one.
     * This function is safe to call multiple times, as it will only update memories that actually need an update.
     * @param cancellationToken A [CancellationToken] that can be used to cancel the operation. If not provided, the operation will not be cancellable.
     */
    fun updateMemoriesIfNeeded(cancellationToken: CancellationToken = CancellationToken.NonCancellable) {
        val memories = Database { MemoryEntity.all() }
        val memoriesToUpdate = memories.filter { memory ->
            memory.pdf == null || needsUpdate(memory.pdf!!)
        }
        if (memoriesToUpdate.isEmpty()) {
            logger.info("No memories need PDF updates")
            return
        }
        logger.info("Found ${memoriesToUpdate.size} memories that need PDF updates")
        for (memory in memoriesToUpdate) {
            logger.info("Updating PDF for memory ${memory.id.value}")
            generateMemoryPdf(memory, cancellationToken)
        }
    }

    /**
     * Generates a PDF for the given memory and saves it to the database.
     * If the memory already has a PDF, it will be replaced.
     */
    fun generateMemoryPdf(memory: MemoryEntity, cancellationToken: CancellationToken = CancellationToken.NonCancellable) {
        val verificationCode = DocumentVerification.newCode()
        val baos = ByteArrayOutputStream()
        baos.use { output ->
            generateMemoryPdf(
                memory,
                photoProvider = { uuid -> Database { FileEntity[uuid] }.readBytes() },
                outputStream = output,
                cancellationToken = cancellationToken,
                verificationCode = verificationCode,
            )
        }

        Database {
            DocumentVerification.record(verificationCode, baos.toByteArray(), DocumentType.MEMORY, memory.id.value)

            val oldPdf = memory.pdf
            memory.pdf = FileEntity.create(
                bytes = baos.toByteArray(),
                name = "memory_${memory.id.value}.pdf",
                contentType = ContentType.Application.Pdf,
                // Best-effort: restricted to the submitter and admins. Department MEMORY_MANAGERs and tagged
                // members can see this memory's data via GET /memories/{id} (see memoryRequest()) but won't be able
                // to download this specific file -- FileReadWriteRules only supports flat user/group lists, not the
                // department-role checks that read access to the memory itself is based on.
                rules = FileReadWriteRules(readUsers = listOf(memory.submittedBy.sub.value), readGroups = listOf(ADMIN_GROUP_NAME)),
            )
            oldPdf?.delete()
        }
    }

    fun generateMemoryPdf(
        memory: MemoryEntity,
        photoProvider: (Uuid) -> ByteArray, // Callback to fetch actual image data
        outputStream: OutputStream,
        cancellationToken: CancellationToken = CancellationToken.NonCancellable,
        verificationCode: String? = null,
    ) {
        val (referencedMemory, itemsUsed, submittedByName) = Database {
            val users = UserReferenceEntity.all().map { it.toData() }
            val departments = DepartmentEntity.all().map { it.toData() }
            val referencedMemory = memory.toData().referenced(
                users = users,
                members = memory.members.map { it.toMember() },
                departments = departments,
            )
            val itemsUsed = memory.lending?.items?.toList().orEmpty().map { item ->
                item.toData().referenced(item.type.toData().referenced(departments))
            }
            Triple(referencedMemory, itemsUsed, memory.submittedBy.fullName)
        }

        generateMemoryPdf(
            memory = referencedMemory,
            itemsUsed = itemsUsed,
            submittedBy = submittedByName,
            photoProvider = photoProvider,
            outputStream = outputStream,
            cancellationToken = cancellationToken,
            verificationCode = verificationCode,
        ) {
            creationDate = Calendar.getInstance().apply {
                time = Date(memory.createdAt.toEpochMilliseconds())
            }
            modificationDate = Calendar.getInstance().apply {
                time = Date(memory.lastUpdate.toEpochMilliseconds())
            }
        }
    }

    fun generateMemoryPdf(
        memory: ReferencedMemory,
        itemsUsed: List<ReferencedInventoryItem>?,
        submittedBy: String,
        photoProvider: (Uuid) -> ByteArray, // Callback to fetch actual image data
        outputStream: OutputStream,
        cancellationToken: CancellationToken = CancellationToken.NonCancellable,
        /** Printed on every page with a link to verify the document (see [DocumentVerification]), if given. */
        verificationCode: String? = null,
        extraMeta: PDDocumentInformation.() -> Unit = {}
    ) {
        PDDocument().use { document ->
            document.documentInformation = PDDocumentInformation().apply {
                title = "Memòria d'Activitat"
                author = submittedBy
                creator = "Centre Excursionista Alcoi"
                subject = "Memòria d'Activitat"
                keywords = "PDF, Memòria, Activitat, CEA"

                setCustomMetadataValue(META_VERSION, VERSION.toString())
                setCustomMetadataValue(META_MEMORY_ID, memory.id.toString())
                setCustomMetadataValue(META_LENDING_ID, memory.lending?.toString())

                extraMeta()
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
                val department = memory.department ?: itemsUsed?.firstNotNullOfOrNull { it.type.department }
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
            if (!itemsUsed.isNullOrEmpty()) {
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

                if (verificationCode != null) {
                    drawVerificationFooter(footerPage, footerStream, verificationCode, fontRegular)
                }
                footerStream.close()
            }

            PdfSigner.save(document, outputStream)
        }
    }

    /**
     * Draws [code] on [page], above the page number, with a link to the page that verifies the document: the whole
     * line can be clicked.
     */
    private fun drawVerificationFooter(page: PDPage, stream: PDPageContentStream, code: String, font: PDFont) {
        val url = DocumentVerification.url(code)
        val text = "Codi de verificació: ${DocumentVerification.format(code)} · Comprova'n l'autenticitat a $url"
        val size = 8f
        val textWidth = font.getStringWidth(text) / 1000 * size
        val x = (page.mediaBox.width - textWidth) / 2
        val y = 32f

        stream.beginText()
        stream.setFont(font, size)
        stream.setNonStrokingColor(Color.GRAY)
        stream.newLineAtOffset(x, y)
        stream.showText(text)
        stream.endText()

        page.annotations.add(
            PDAnnotationLink().apply {
                rectangle = PDRectangle(x, y - 2, textWidth, size + 4)
                borderStyle = PDBorderStyleDictionary().apply { width = 0f }
                action = PDActionURI().apply { uri = url }
            }
        )
    }
}
