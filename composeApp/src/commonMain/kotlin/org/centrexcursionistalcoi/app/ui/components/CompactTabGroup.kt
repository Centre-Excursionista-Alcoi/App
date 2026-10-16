package org.centrexcursionistalcoi.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Handyman
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.HolidayVillage
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.theme.ColorPalette
import org.centrexcursionistalcoi.app.ui.theme.font.PlusJakartaSans

typealias CompactTabGroupItem = Pair<ImageVector, @Composable () -> String>

data class CompactTabGroupStyle(
    val activeItemBackgroundColor: Color,
    val activeItemContentColor: Color,
    val idleItemBackgroundColor: Color,
    val idleItemContentColor: Color,
    val shape: Shape,
) {
    companion object {
        val Default: CompactTabGroupStyle = CompactTabGroupStyle(
            activeItemBackgroundColor = ColorPalette.primary,
            activeItemContentColor = ColorPalette.onPrimary,
            idleItemBackgroundColor = ColorPalette.containerNeutral,
            idleItemContentColor = ColorPalette.onContainerNeutral,
            shape = RoundedCornerShape(50)
        )
    }
}

@Composable
fun CompactTabGroup(
    selectedItem: Int,
    items: List<CompactTabGroupItem>,
    onItemSelected: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    style: CompactTabGroupStyle = CompactTabGroupStyle.Default,
) {
    Surface(
        modifier = modifier,
        color = style.idleItemBackgroundColor,
        contentColor = style.idleItemContentColor,
        shape = style.shape,
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            val tabWidth = maxWidth / items.size

            val activeItemX by animateDpAsState(
                targetValue = tabWidth * selectedItem,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "activeItemX"
            )

            // 1. Sliding background indicator (renders behind)
            Box(modifier = Modifier.matchParentSize()) {
                Box(
                    modifier = Modifier
                        .offset(x = activeItemX)
                        .width(tabWidth)
                        .fillMaxHeight() // Matches the height of the Row automatically
                        .background(
                            color = style.activeItemBackgroundColor,
                            shape = style.shape,
                        )
                )
            }

            // 2. Actual items display
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for ((index, item) in items.withIndex()) {
                    val isActive = index == selectedItem
                    val (icon, textRes) = item

                    val color by animateColorAsState(
                        targetValue = if (isActive) style.activeItemContentColor else style.idleItemContentColor,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "textColor$index"
                    )

                    Row(
                        modifier = Modifier
                            .clickable { onItemSelected(index) }
                            .weight(1f)
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = textRes(),
                            tint = color
                        )
                        Text(
                            text = textRes(),
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = color,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(widthDp = 700)
@Composable
fun CompactTabGroup_Preview() {
    var selectedItem by remember { mutableStateOf(0) }

    CompactTabGroup(
        selectedItem = selectedItem,
        items = listOf(
            (MaterialSymbols.Handyman to { "Material & Gear" }),
            (MaterialSymbols.HolidayVillage to { "Spaces & Refuges" })
        ),
        onItemSelected = { selectedItem = it }
    )
}
