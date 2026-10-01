package org.centrexcursionistalcoi.app.pdf

import org.apache.fontbox.ttf.TrueTypeCollection
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts

/**
 * A typeface in its regular, bold, italic and bold italic variants ([get]). Each variant is only loaded (and so
 * embedded in the document) once used.
 */
internal class FontFamily(
    regular: () -> PDFont,
    bold: () -> PDFont,
    italic: () -> PDFont,
    boldItalic: () -> PDFont,
) {
    val regular: PDFont by lazy(regular)
    private val bold by lazy(bold)
    private val italic by lazy(italic)
    private val boldItalic by lazy(boldItalic)

    operator fun get(bold: Boolean = false, italic: Boolean = false): PDFont = when {
        bold && italic -> this.boldItalic
        bold -> this.bold
        italic -> this.italic
        else -> this.regular
    }
}

/**
 * The fonts of the PDFs, for [document]. Every weight of each typeface, upright and italic, is available in its
 * collection in `resources/fonts` (`RobotoCondensed.ttc`, `Nunito.ttc`), named like `Nunito-SemiBoldItalic`.
 *
 * The collections are read into memory whole, so there's nothing to close: they're released with this object.
 */
internal class PdfFonts(private val document: PDDocument) {
    private val robotoCondensed = collection("RobotoCondensed")
    private val nunito = collection("Nunito")

    /** Body text. */
    val body = FontFamily(
        regular = { robotoCondensed.load("RobotoCondensed-Light") },
        bold = { robotoCondensed.load("RobotoCondensed-Bold") },
        italic = { robotoCondensed.load("RobotoCondensed-LightItalic") },
        boldItalic = { robotoCondensed.load("RobotoCondensed-BoldItalic") },
    )

    /** Titles and headings: bold already, so their bold is heavier. */
    val titles = FontFamily(
        regular = { nunito.load("Nunito-Bold") },
        bold = { nunito.load("Nunito-ExtraBold") },
        italic = { nunito.load("Nunito-BoldItalic") },
        boldItalic = { nunito.load("Nunito-ExtraBoldItalic") },
    )

    /** Code: monospaced, one of the fonts every PDF reader has, so nothing is embedded. */
    val code = FontFamily(
        regular = { PDType1Font(Standard14Fonts.FontName.COURIER) },
        bold = { PDType1Font(Standard14Fonts.FontName.COURIER_BOLD) },
        italic = { PDType1Font(Standard14Fonts.FontName.COURIER_OBLIQUE) },
        boldItalic = { PDType1Font(Standard14Fonts.FontName.COURIER_BOLD_OBLIQUE) },
    )

    private fun collection(name: String): TrueTypeCollection {
        val stream = checkNotNull(PdfFonts::class.java.getResourceAsStream("/fonts/$name.ttc")) { "Font collection $name not found" }
        return stream.use { TrueTypeCollection(it) }
    }

    private fun TrueTypeCollection.load(name: String): PDFont {
        val font = checkNotNull(getFontByName(name)) { "Font $name not found" }
        return PDType0Font.load(document, font, true)
    }
}
