package org.centrexcursionistalcoi.app.pdf

import kotlinx.datetime.TimeZone
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import org.centrexcursionistalcoi.app.data.Memory
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.utils.toUuid
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * The text of a memory, Markdown, as drawn by [PdfGeneratorService.generateMemoryPdf].
 */
class TestPdfMarkdown {
    private class Glyph(val text: String, val font: String, val size: Float, val right: Float)

    private class Pdf(val text: String, val glyphs: List<Glyph>, val pages: Int, val pageWidth: Float)

    private fun pdf(markdown: String): Pdf {
        val now = ZonedDateTime.fromInstant(Clock.System.now(), TimeZone.currentSystemDefault())
        val memory = Memory(
            id = "9b57a238-6a3a-4a1a-9f4a-6f4b1e6f5a11".toUuid(),
            place = null,
            members = emptyList(),
            externalUsers = null,
            text = markdown,
            sport = null,
            department = null,
            attachments = emptyList(),
            submittedBy = FakeUser.SUB,
            from = now,
            to = now,
            pdf = null,
            lending = null,
        )
        val bytes = ByteArrayOutputStream().use { output ->
            PdfGeneratorService.generateMemoryPdf(
                memory = memory.referenced(listOf(FakeUser.data()), emptyList(), emptyList()),
                itemsUsed = emptyList(),
                submittedBy = "Admin User",
                photoProvider = { error("No photos") },
                outputStream = output,
            )
            output.toByteArray()
        }
        return Loader.loadPDF(bytes).use { document ->
            val glyphs = mutableListOf<Glyph>()
            val stripper = object : PDFTextStripper() {
                override fun writeString(text: String, positions: List<TextPosition>) {
                    for (position in positions) {
                        glyphs += Glyph(position.unicode, position.font.name, position.fontSizeInPt, position.xDirAdj + position.widthDirAdj)
                    }
                    super.writeString(text, positions)
                }
            }
            stripper.lineSeparator = "\n"
            Pdf(stripper.getText(document), glyphs, document.numberOfPages, document.getPage(0).mediaBox.width)
        }
    }

    /** The glyphs of the first occurrence of [word] in the document. */
    private fun Pdf.glyphsOf(word: String): List<Glyph> {
        val all = glyphs.joinToString("") { it.text }
        val index = all.indexOf(word)
        assertTrue(index >= 0, "\"$word\" not found in: $all")
        return glyphs.subList(index, index + word.length)
    }

    @Test
    fun test_syntaxIsNotPrinted() {
        val pdf = pdf("# Títol\nAmb **negreta**, *cursiva*, ~~ratllat~~, `codi` i [un enllaç](https://example.com).\n\\*Sense cursiva\\*")

        for (text in listOf("Títol", "negreta", "cursiva", "ratllat", "codi", "un enllaç", "*Sense cursiva*")) {
            assertContains(pdf.text, text)
        }
        for (syntax in listOf("#", "**", "~~", "`", "](", "https://example.com", "\\")) {
            assertFalse(syntax in pdf.text, "\"$syntax\" printed in: ${pdf.text}")
        }
    }

    @Test
    fun test_singleLineBreak_startsNewLine() {
        val lines = pdf("Primera línia\nSegona línia\n\nAltre paràgraf").text.lines()

        assertContains(lines, "Primera línia")
        assertContains(lines, "Segona línia")
        assertContains(lines, "Altre paràgraf")
    }

    @Test
    fun test_lists() {
        val lines = pdf("- U\n- Dos\n\n1. Primer\n2. Segon").text.lines()

        assertTrue(lines.any { it.matches(Regex("""•\s*U""")) }, "No bullet in: $lines")
        assertTrue(lines.any { it.matches(Regex("""•\s*Dos""")) }, "No bullet in: $lines")
        assertTrue(lines.any { it.matches(Regex("""1\.\s*Primer""")) }, "No number in: $lines")
        assertTrue(lines.any { it.matches(Regex("""2\.\s*Segon""")) }, "No number in: $lines")
    }

    @Test
    fun test_styles() {
        val pdf = pdf("# Capçalera\nText normal, **negreta**, *cursiva*, ***totes dues*** i `codi`")

        // Fonts embedded as subsets are named like "ABCDEF+RobotoCondensed-Bold"
        fun List<Glyph>.assertFont(name: String) =
            assertTrue(all { it.font.substringAfter('+') == name }, "Expected $name, got: ${map { it.font }.distinct()}")

        val heading = pdf.glyphsOf("Capçalera")
        val body = pdf.glyphsOf("normal")
        heading.assertFont("Nunito-Bold")
        assertTrue(heading.first().size > body.first().size, "Heading not larger than body text")
        body.assertFont("RobotoCondensed-Light")
        pdf.glyphsOf("negreta").assertFont("RobotoCondensed-Bold")
        pdf.glyphsOf("cursiva").assertFont("RobotoCondensed-LightItalic")
        pdf.glyphsOf("totes dues").filter { it.text.isNotBlank() }.assertFont("RobotoCondensed-BoldItalic")
        pdf.glyphsOf("codi").assertFont("Courier")
    }

    @Test
    fun test_unsupportedCharacters_drawnAsQuestionMarks() {
        val pdf = pdf("Molt bé 🧗 i `codi π`")

        assertContains(pdf.text, "Molt bé ? i")
        assertContains(pdf.text, "codi ?")
    }

    @Test
    fun test_longText_wrapsWithinMarginsAndBreaksPages() {
        val paragraph = "Una activitat molt llarga amb paraules suficients per a omplir diverses línies de text. ".repeat(8)
        val pdf = pdf(List(40) { paragraph }.joinToString("\n\n") + "\n\n" + "Paraulallarguíssima".repeat(20))

        assertTrue(pdf.pages > 1, "Expected several pages, got ${pdf.pages}")
        val overflow = pdf.glyphs.filter { it.right > pdf.pageWidth - 50f + 1f }
        assertTrue(overflow.isEmpty(), "Text past the right margin: ${overflow.joinToString("") { it.text }}")
    }
}
