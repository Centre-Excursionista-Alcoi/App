package org.centrexcursionistalcoi.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.timeline_status_done
import cea_app.composeapp.generated.resources.timeline_status_in_progress
import cea_app.composeapp.generated.resources.timeline_status_pending
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Check
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Pending
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.PendingActions
import org.centrexcursionistalcoi.app.ui.theme.ColorPalette
import org.centrexcursionistalcoi.app.ui.theme.font.PlusJakartaSans
import org.jetbrains.compose.resources.stringResource

data class TimelineItem(
    val title: @Composable () -> String,
    val content: @Composable ColumnScope.() -> Unit,
    val status: TimelineItemStatus,
    val supportingContent: (@Composable () -> Unit)? = null,
)

enum class TimelineItemStatus {
    Pending, InProgress, Done
}

@Composable
fun Timeline(
    items: List<TimelineItem>,
    modifier: Modifier = Modifier,
    addStepNumber: Boolean = true
) {
    val density = LocalDensity.current

    Box(modifier = modifier) {
        var itemsHeight by remember { mutableStateOf(0.dp) }

        Box(
            modifier = Modifier
                .width(2.dp)
                .height(itemsHeight - 42.dp)
                .offset(x = 19.dp, y = 24.dp)
                .background(ColorPalette.neutral)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned {
                    itemsHeight = with(density) { it.size.height.toDp() }
                }
        ) {
            for ((index, item) in items.withIndex()) {
                val style = when (item.status) {
                    TimelineItemStatus.Done -> ColorPalette.Style.containerNeutral
                    TimelineItemStatus.InProgress -> ColorPalette.Style.containerWarning
                    TimelineItemStatus.Pending -> ColorPalette.Style.containerNeutralDisabled
                }

                Row(modifier = Modifier.padding(vertical = 6.dp)) {
                    Surface(
                        color = when (item.status) {
                            TimelineItemStatus.Done -> ColorPalette.success
                            TimelineItemStatus.InProgress -> ColorPalette.warning
                            TimelineItemStatus.Pending -> ColorPalette.neutral
                        },
                        shape = CircleShape,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(
                            imageVector = when (item.status) {
                                TimelineItemStatus.Done -> MaterialSymbols.Check
                                TimelineItemStatus.Pending -> MaterialSymbols.Pending
                                TimelineItemStatus.InProgress -> MaterialSymbols.PendingActions
                            },
                            contentDescription = stringResource(
                                when (item.status) {
                                    TimelineItemStatus.Pending -> Res.string.timeline_status_pending
                                    TimelineItemStatus.InProgress -> Res.string.timeline_status_in_progress
                                    TimelineItemStatus.Done -> Res.string.timeline_status_done
                                }
                            ),
                            tint = when (item.status) {
                                TimelineItemStatus.Done -> Color.White
                                TimelineItemStatus.Pending -> Color.White
                                TimelineItemStatus.InProgress -> ColorPalette.onNeutral
                            },
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f).heightIn(min = 36.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            style.Providing(
                                color = if (item.status == TimelineItemStatus.InProgress)
                                    ColorPalette.warning
                                else
                                    Color.Unspecified,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                            ) {
                                val prefix = if (addStepNumber) "${index + 1}. " else ""
                                Text(
                                    text = prefix + item.title(),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            style.Providing(fontWeight = FontWeight.Light) {
                                item.supportingContent?.invoke()
                            }
                        }

                        style.Providing {
                            item.content(this)
                        }
                    }
                }
            }
        }
    }
}

@Preview(widthDp = 600, showBackground = true)
@Composable
fun TimelinePreview() {
    Timeline(
        items = listOf(
            TimelineItem(
                title = { "Sol·licitud i Assegurança Validada" },
                content = { Text("Aprovat per la secció d'escalada. Llicència FEDME B activa.") },
                supportingContent = { Text("08 Mar, 10:15h") },
                status = TimelineItemStatus.Done,
            ),
            TimelineItem(
                title = { "Recollit" },
                content = { Text("Lliurat per Joan Carles Soler") },
                supportingContent = { Text("09 Mar, 19:30h") },
                status = TimelineItemStatus.Done,
            ),
            TimelineItem(
                title = { "En Ús - Pendent de Retorn" },
                content = {
                    Text(
                        buildAnnotatedString {
                            append("Data límit: ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = ColorPalette.warning)) {
                                append("12 Març 2025, 20:00h")
                            }
                        }
                    )
                    Text("LLoc: Centre Excursionista d'Alcoi")
                },
                supportingContent = { Text("09 Mar, 19:30h") },
                status = TimelineItemStatus.InProgress,
            ),
            TimelineItem(
                title = { "Memòria d'Activitat" },
                content = { Text("Formulari post-activitat") },
                supportingContent = { Text("Pendent") },
                status = TimelineItemStatus.Pending,
            ),
            TimelineItem(
                title = { "Préstec completat" },
                content = {},
                supportingContent = { Text("Pendent") },
                status = TimelineItemStatus.Pending,
            ),
        )
    )
}
