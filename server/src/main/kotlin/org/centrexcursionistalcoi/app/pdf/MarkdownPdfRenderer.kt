package org.centrexcursionistalcoi.app.pdf

import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.util.Matrix
import org.centrexcursionistalcoi.app.pdf.PdfGeneratorService.MARGIN
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.CancellationToken
import org.intellij.markdown.parser.MarkdownParser
import java.awt.Color

/**
 * Draws Markdown at the cursor of [context], styled and wrapped to the page's width, breaking pages as needed (see
 * [draw]).
 *
 * Like the app (see `MemoryDialog`), a single line break starts a new line. Headings are drawn with the heading
 * font, code with the code font, and bold and italic text with the bold and italic variants of each ([FontFamily]).
 * Characters a font can't draw (e.g. emojis) are drawn as `?`.
 */
internal class MarkdownPdfRenderer(private val context: DrawContext) {
    fun draw(
        markdownText: CharSequence,
        font: FontFamily,
        size: Float,
        color: Color = Color.BLACK,
        headingFont: FontFamily = font,
        codeFont: FontFamily,
        flavour: MarkdownFlavourDescriptor = GFMFlavourDescriptor(),
        cancellationToken: CancellationToken = CancellationToken.NonCancellable
    ) {
        val tree = MarkdownParser(
            flavour = flavour,
            cancellationToken = cancellationToken
        ).buildMarkdownTreeFromString(markdownText)
        val markdown = MarkdownStyles(markdownText.toString(), TextStyle(font, size, color), headingFont, codeFont)
        drawBlocks(tree.children, markdown, indent = 0f, blockGap = size * 0.6f)
    }

    /** A list item's marker, drawn on the first line its contents draw (see [drawLine]). */
    private var pendingMarker: Pair<Float, LineSegment>? = null

    private fun drawBlocks(blocks: List<ASTNode>, markdown: MarkdownStyles, indent: Float, blockGap: Float) {
        val base = markdown.base
        for (block in blocks) {
            when (block.type) {
                MarkdownElementTypes.PARAGRAPH -> {
                    drawRuns(inlineRuns(block, markdown, base), indent)
                    context.moveDown(blockGap)
                }
                MarkdownElementTypes.ATX_1, MarkdownElementTypes.SETEXT_1 -> drawHeading(block, markdown, 1.6f, indent)
                MarkdownElementTypes.ATX_2, MarkdownElementTypes.SETEXT_2 -> drawHeading(block, markdown, 1.4f, indent)
                MarkdownElementTypes.ATX_3 -> drawHeading(block, markdown, 1.2f, indent)
                MarkdownElementTypes.ATX_4, MarkdownElementTypes.ATX_5, MarkdownElementTypes.ATX_6 ->
                    drawHeading(block, markdown, 1.1f, indent)
                MarkdownElementTypes.UNORDERED_LIST, MarkdownElementTypes.ORDERED_LIST -> {
                    for (item in block.children.filter { it.type == MarkdownElementTypes.LIST_ITEM }) {
                        drawListItem(item, markdown, indent)
                    }
                    context.moveDown(blockGap)
                }
                MarkdownElementTypes.BLOCK_QUOTE -> {
                    val quote = markdown.copy(base = base.copy(color = Color.GRAY, italic = true))
                    drawBlocks(block.children, quote, indent + base.size * 1.5f, blockGap)
                }
                MarkdownElementTypes.CODE_FENCE -> {
                    val content = block.children
                        .dropWhile { it.type != MarkdownTokenTypes.EOL }.drop(1)
                        .takeWhile { it.type != MarkdownTokenTypes.CODE_FENCE_END }
                        .joinToString("") { it.getTextInNode(markdown.source) }
                    drawCode(content.removeSuffix("\n"), markdown, indent)
                    context.moveDown(blockGap)
                }
                MarkdownElementTypes.CODE_BLOCK -> {
                    val content = block.getTextInNode(markdown.source).lines().joinToString("\n") { line ->
                        line.removePrefix(line.take(4).takeWhile { it == ' ' })
                    }
                    drawCode(content, markdown, indent)
                    context.moveDown(blockGap)
                }
                MarkdownTokenTypes.HORIZONTAL_RULE -> {
                    context.checkPageBreak(base.size)
                    val y = context.yPosition - base.size / 2
                    context.contentStream.setStrokingColor(Color.LIGHT_GRAY)
                    context.contentStream.setLineWidth(0.5f)
                    context.contentStream.moveTo(MARGIN + indent, y)
                    context.contentStream.lineTo(MARGIN + context.width, y)
                    context.contentStream.stroke()
                    context.moveDown(base.size + blockGap)
                }
                GFMElementTypes.TABLE -> {
                    for (row in block.children.filter { it.type == GFMElementTypes.HEADER || it.type == GFMElementTypes.ROW }) {
                        val style = if (row.type == GFMElementTypes.HEADER) base.copy(bold = true) else base
                        val runs = mutableListOf<InlineRun>()
                        row.children.filter { it.type == GFMTokenTypes.CELL }.forEachIndexed { i, cell ->
                            if (i > 0) runs += InlineRun.Text(" | ", base)
                            runs += inlineRuns(cell, markdown, style)
                        }
                        drawRuns(runs, indent)
                    }
                    context.moveDown(blockGap)
                }
                // Separators between blocks, a quote's markers, and link definitions, which show nothing
                MarkdownTokenTypes.EOL, MarkdownTokenTypes.WHITE_SPACE, MarkdownTokenTypes.BLOCK_QUOTE,
                MarkdownElementTypes.LINK_DEFINITION -> Unit
                else -> {
                    // Anything else (e.g. HTML), as plain text
                    drawRuns(plainRuns(block.getTextInNode(markdown.source).toString().trim(), base), indent)
                    context.moveDown(blockGap)
                }
            }
        }
    }

    private fun drawHeading(heading: ASTNode, markdown: MarkdownStyles, scale: Float, indent: Float) {
        val base = markdown.base
        val content = heading.children.firstOrNull {
            it.type == MarkdownTokenTypes.ATX_CONTENT || it.type == MarkdownTokenTypes.SETEXT_CONTENT
        } ?: return
        val style = base.copy(family = markdown.headingFont, size = base.size * scale)
        context.moveDown(base.size * 0.4f)
        drawRuns(inlineRuns(content, markdown, style).dropWhile { it is InlineRun.Text && it.text.isBlank() }, indent)
        context.moveDown(base.size * 0.2f)
    }

    private fun drawListItem(item: ASTNode, markdown: MarkdownStyles, indent: Float) {
        val base = markdown.base
        val marker = item.children.firstOrNull { it.type == MarkdownTokenTypes.LIST_NUMBER }
            ?.getTextInNode(markdown.source)?.trim()?.toString()
            ?: "•"
        val markerWidth = base.size * 1.6f
        pendingMarker = MARGIN + indent to LineSegment(printable(marker, base.font), base)
        drawBlocks(
            item.children.filter { it.type != MarkdownTokenTypes.LIST_BULLET && it.type != MarkdownTokenTypes.LIST_NUMBER },
            markdown,
            indent + markerWidth,
            blockGap = base.size * 0.2f,
        )
        // An empty item: just its marker
        if (pendingMarker != null) drawLine(emptyList(), MARGIN + indent + markerWidth, base.size)
    }

    private fun drawCode(code: String, markdown: MarkdownStyles, indent: Float) {
        val style = markdown.code(markdown.base)
        for (line in code.lines()) {
            // Code keeps its spaces, so it's wrapped by characters, not by words
            var rest = printable(line, style.font)
            do {
                var count = rest.length
                while (count > 1 && style.width(rest.take(count)) > context.width - indent) count--
                drawLine(listOf(LineSegment(rest.take(count), style)), MARGIN + indent, style.size)
                rest = rest.drop(count)
            } while (rest.isNotEmpty())
        }
    }

    /** The text (and line breaks) of an inline [node]'s children, styled. */
    private fun inlineRuns(node: ASTNode, markdown: MarkdownStyles, style: TextStyle): List<InlineRun> {
        val runs = mutableListOf<InlineRun>()
        fun children(of: ASTNode, style: TextStyle, skip: Set<IElementType>) {
            for (child in of.children) {
                if (child.type in skip) continue
                runs += inlineRuns(child, markdown, style)
            }
        }
        fun nodeText() = node.getTextInNode(markdown.source).toString()
        when (node.type) {
            MarkdownElementTypes.EMPH -> children(node, style.copy(italic = true), setOf(MarkdownTokenTypes.EMPH))
            MarkdownElementTypes.STRONG -> children(node, style.copy(bold = true), setOf(MarkdownTokenTypes.EMPH))
            GFMElementTypes.STRIKETHROUGH -> children(node, style.copy(strikethrough = true), setOf(GFMTokenTypes.TILDE))
            MarkdownElementTypes.CODE_SPAN -> {
                val code = node.children.filter { it.type != MarkdownTokenTypes.BACKTICK }
                    .joinToString("") { it.getTextInNode(markdown.source) }
                    .replace('\n', ' ')
                runs += InlineRun.Text(code, markdown.code(style))
            }
            MarkdownElementTypes.INLINE_LINK, MarkdownElementTypes.FULL_REFERENCE_LINK,
            MarkdownElementTypes.SHORT_REFERENCE_LINK -> {
                val label = node.children.firstOrNull {
                    it.type == MarkdownElementTypes.LINK_TEXT || it.type == MarkdownElementTypes.LINK_LABEL
                }
                if (label != null) children(label, style.copy(color = LINK_COLOR), setOf(MarkdownTokenTypes.LBRACKET, MarkdownTokenTypes.RBRACKET))
            }
            MarkdownElementTypes.AUTOLINK ->
                children(node, style.copy(color = LINK_COLOR), setOf(MarkdownTokenTypes.LT, MarkdownTokenTypes.GT))
            // Not drawn: the PDF has its own photos section
            MarkdownElementTypes.IMAGE -> Unit
            // Delimiters of a quote's continuation lines
            MarkdownTokenTypes.BLOCK_QUOTE -> Unit
            MarkdownTokenTypes.EOL, MarkdownTokenTypes.HARD_LINE_BREAK -> runs += InlineRun.LineBreak
            MarkdownTokenTypes.WHITE_SPACE -> runs += InlineRun.Text(" ", style)
            MarkdownTokenTypes.TEXT -> runs += InlineRun.Text(nodeText().replace(ESCAPE, "$1"), style)
            MarkdownTokenTypes.AUTOLINK, MarkdownTokenTypes.EMAIL_AUTOLINK, MarkdownTokenTypes.URL,
            GFMTokenTypes.GFM_AUTOLINK -> runs += InlineRun.Text(nodeText(), style.copy(color = LINK_COLOR))
            else -> if (node.children.isEmpty()) runs += InlineRun.Text(nodeText(), style) else children(node, style, emptySet())
        }
        return runs
    }

    private fun plainRuns(text: String, style: TextStyle): List<InlineRun> =
        text.lines().flatMapIndexed { i, line ->
            listOfNotNull(InlineRun.LineBreak.takeIf { i > 0 }, InlineRun.Text(line, style))
        }

    /** Draws [runs] wrapped by words to the context.width left after [indent], each line break starting a new line. */
    private fun drawRuns(runs: List<InlineRun>, indent: Float) {
        val maxWidth = context.width - indent
        val line = mutableListOf<LineSegment>()
        var lineWidth = 0f
        var lineSize = 0f

        fun flush() {
            while (line.lastOrNull()?.text?.isBlank() == true) line.removeAt(line.lastIndex)
            drawLine(line.toList(), MARGIN + indent, lineSize)
            line.clear()
            lineWidth = 0f
            lineSize = 0f
        }

        for (run in runs) {
            if (run is InlineRun.LineBreak) {
                lineSize = maxOf(lineSize, runs.firstNotNullOfOrNull { (it as? InlineRun.Text)?.style?.size } ?: 0f)
                flush()
                continue
            }
            run as InlineRun.Text
            val style = run.style
            lineSize = maxOf(lineSize, style.size)
            for (piece in WORD.findAll(printable(run.text.replace('\t', ' '), style.font)).map { it.value }) {
                if (piece.isBlank()) {
                    // No spaces at the start of a line
                    if (line.isNotEmpty()) {
                        line += LineSegment(" ", style)
                        lineWidth += style.width(" ")
                    }
                    continue
                }
                var word = piece
                if (line.isNotEmpty() && lineWidth + style.width(word) > maxWidth) flush()
                // A word longer than a whole line, split wherever it fills one
                while (style.width(word) > maxWidth - lineWidth && word.length > 1) {
                    var count = word.length - 1
                    while (count > 1 && style.width(word.take(count)) > maxWidth - lineWidth) count--
                    line += LineSegment(word.take(count), style)
                    lineSize = maxOf(lineSize, style.size)
                    flush()
                    word = word.drop(count)
                }
                line += LineSegment(word, style)
                lineWidth += style.width(word)
                lineSize = maxOf(lineSize, style.size)
            }
        }
        if (line.isNotEmpty()) flush()
    }

    /**
     * Draws [segments] as one line starting at [x], with the pending list marker if any, and moves the cursor
     * below it. Like [DrawContext.drawText], the cursor is the line's baseline.
     */
    private fun drawLine(segments: List<LineSegment>, x: Float, lineSize: Float) {
        val size = if (lineSize > 0f) lineSize else segments.maxOfOrNull { it.style.size } ?: 0f
        context.checkPageBreak(size + 2)
        // Each segment, with where it starts
        val drawn = mutableListOf<Pair<Float, LineSegment>>()
        pendingMarker?.let { drawn += it }
        pendingMarker = null
        var left = x
        for (segment in segments) {
            drawn += left to segment
            left += segment.style.width(segment.text)
        }

        context.contentStream.beginText()
        for ((start, segment) in drawn) {
            val style = segment.style
            context.contentStream.setFont(style.font, style.size)
            context.contentStream.setNonStrokingColor(style.color)
            context.contentStream.setTextMatrix(Matrix.getTranslateInstance(start, context.yPosition))
            context.contentStream.showText(segment.text)
        }
        context.contentStream.endText()

        for ((start, segment) in drawn.filter { it.second.style.strikethrough }) {
            val style = segment.style
            val y = context.yPosition + style.size * 0.3f
            context.contentStream.setStrokingColor(style.color)
            context.contentStream.setLineWidth(style.size * 0.05f)
            context.contentStream.moveTo(start, y)
            context.contentStream.lineTo(start + style.width(segment.text), y)
            context.contentStream.stroke()
        }
        context.moveDown(size + 4)
    }

    private val drawable = mutableMapOf<PDFont, MutableMap<Int, Boolean>>()

    /** [text], with the characters [font] can't draw replaced with `?`. */
    private fun printable(text: String, font: PDFont): String {
        val known = drawable.getOrPut(font) { mutableMapOf() }
        return buildString {
            text.codePoints().forEach { codePoint ->
                val char = String(Character.toChars(codePoint))
                val canDraw = known.getOrPut(codePoint) {
                    try {
                        font.encode(char)
                        true
                    } catch (_: IllegalArgumentException) {
                        false
                    }
                }
                append(if (canDraw) char else "?")
            }
        }
    }
}

private val LINK_COLOR = Color(0x15, 0x65, 0xC0)

/** A backslash escaping a punctuation character, kept as-is by the parser in the text. */
private val ESCAPE = Regex("""\\([!-/:-@\[-`{-~])""")

/** Words and the spaces between them. */
private val WORD = Regex("""\s+|\S+""")

/** How a piece of text is drawn: with the variant of [family] for [bold] and [italic] ([font]). */
private data class TextStyle(
    val family: FontFamily,
    val size: Float,
    val color: Color,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val strikethrough: Boolean = false,
) {
    val font: PDFont get() = family[bold, italic]

    fun width(text: String): Float = font.getStringWidth(text) / 1000 * size
}

private data class MarkdownStyles(
    val source: String,
    val base: TextStyle,
    val headingFont: FontFamily,
    val codeFont: FontFamily,
) {
    /** [style] for code: [codeFont] is monospaced, and so looks larger than text of the same size. */
    fun code(style: TextStyle) = style.copy(family = codeFont, size = style.size * 0.9f)
}

private sealed interface InlineRun {
    data class Text(val text: String, val style: TextStyle) : InlineRun
    data object LineBreak : InlineRun
}

private data class LineSegment(val text: String, val style: TextStyle)
