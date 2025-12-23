package org.centrexcursionistalcoi.app.ui.reusable.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextState
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.*

@OptIn(ExperimentalRichTextApi::class)
@Composable
fun RichTextStyleRow(
    modifier: Modifier = Modifier,
    state: RichTextState,
    enabled: Boolean = true,
) {
    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        item {
            RichTextStyleButton(
                onClick = {
                    state.addParagraphStyle(
                        ParagraphStyle(
                            textAlign = TextAlign.Left,
                        )
                    )
                },
                enabled = enabled,
                isSelected = state.currentParagraphStyle.textAlign == TextAlign.Left,
                icon = MaterialSymbols.FormatAlignLeft
            )
        }

        item {
            RichTextStyleButton(
                onClick = {
                    state.addParagraphStyle(
                        ParagraphStyle(
                            textAlign = TextAlign.Center
                        )
                    )
                },
                enabled = enabled,
                isSelected = state.currentParagraphStyle.textAlign == TextAlign.Center,
                icon = MaterialSymbols.FormatAlignCenter
            )
        }

        item {
            RichTextStyleButton(
                onClick = {
                    state.addParagraphStyle(
                        ParagraphStyle(
                            textAlign = TextAlign.Right
                        )
                    )
                },
                enabled = enabled,
                isSelected = state.currentParagraphStyle.textAlign == TextAlign.Right,
                icon = MaterialSymbols.FormatAlignRight
            )
        }

        item {
            RichTextStyleButton(
                onClick = {
                    state.toggleSpanStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                enabled = enabled,
                isSelected = state.currentSpanStyle.fontWeight == FontWeight.Bold,
                icon = MaterialSymbols.FormatBold
            )
        }

        item {
            RichTextStyleButton(
                onClick = {
                    state.toggleSpanStyle(
                        SpanStyle(
                            fontStyle = FontStyle.Italic
                        )
                    )
                },
                enabled = enabled,
                isSelected = state.currentSpanStyle.fontStyle == FontStyle.Italic,
                icon = MaterialSymbols.FormatItalic
            )
        }

        item {
            RichTextStyleButton(
                onClick = {
                    state.toggleSpanStyle(
                        SpanStyle(
                            textDecoration = TextDecoration.Underline
                        )
                    )
                },
                enabled = enabled,
                isSelected = state.currentSpanStyle.textDecoration?.contains(TextDecoration.Underline) == true,
                icon = MaterialSymbols.FormatUnderlined
            )
        }

        item {
            RichTextStyleButton(
                onClick = {
                    state.toggleSpanStyle(
                        SpanStyle(
                            textDecoration = TextDecoration.LineThrough
                        )
                    )
                },
                enabled = enabled,
                isSelected = state.currentSpanStyle.textDecoration?.contains(TextDecoration.LineThrough) == true,
                icon = MaterialSymbols.FormatStrikethrough
            )
        }

        item {
            RichTextStyleButton(
                onClick = {
                    state.toggleSpanStyle(
                        SpanStyle(
                            fontSize = 28.sp
                        )
                    )
                },
                enabled = enabled,
                isSelected = state.currentSpanStyle.fontSize == 28.sp,
                icon = MaterialSymbols.FormatSize
            )
        }

        item {
            RichTextStyleButton(
                onClick = {
                    state.toggleSpanStyle(
                        SpanStyle(
                            color = Color.Red
                        )
                    )
                },
                enabled = enabled,
                isSelected = state.currentSpanStyle.color == Color.Red,
                icon = MaterialSymbols.Circle,
                tint = Color.Red
            )
        }

        item {
            RichTextStyleButton(
                onClick = {
                    state.toggleSpanStyle(
                        SpanStyle(
                            background = Color.Yellow
                        )
                    )
                },
                enabled = enabled,
                isSelected = state.currentSpanStyle.background == Color.Yellow,
                icon = MaterialSymbols.Circle,
                tint = Color.Yellow
            )
        }

        item {
            Box(
                Modifier
                    .height(24.dp)
                    .width(1.dp)
                    .background(Color(0xFF393B3D))
            )
        }

        item {
            RichTextStyleButton(
                onClick = {
                    state.toggleUnorderedList()
                },
                enabled = enabled,
                isSelected = state.isUnorderedList,
                icon = MaterialSymbols.FormatListBulleted,
            )
        }

        item {
            RichTextStyleButton(
                onClick = {
                    state.toggleOrderedList()
                },
                enabled = enabled,
                isSelected = state.isOrderedList,
                icon = MaterialSymbols.FormatListNumbered,
            )
        }

        item {
            Box(
                Modifier
                    .height(24.dp)
                    .width(1.dp)
                    .background(Color(0xFF393B3D))
            )
        }

        item {
            RichTextStyleButton(
                onClick = {
                    state.addRichSpan(SpellCheck)
                },
                enabled = enabled,
                isSelected = state.isRichSpan<SpellCheck>(),
                icon = MaterialSymbols.Spellcheck,
            )
        }

        item {
            RichTextStyleButton(
                onClick = {
                    state.toggleCodeSpan()
                },
                enabled = enabled,
                isSelected = state.isCodeSpan,
                icon = MaterialSymbols.Code,
            )
        }
    }
}
