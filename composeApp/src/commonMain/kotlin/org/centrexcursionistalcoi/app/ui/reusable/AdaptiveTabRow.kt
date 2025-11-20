package org.centrexcursionistalcoi.app.ui.reusable

import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.centrexcursionistalcoi.app.ui.platform.calculateWindowSizeClass
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

data class TabData(
    val titleRes: StringResource,
    val icon: ImageVector,
    val contentDescription: StringResource? = titleRes,
) {
    @Composable
    fun TabWithIcon(selected: Boolean, onClick: () -> Unit) {
        Tab(
            selected = selected,
            onClick = onClick,
            icon = { Icon(icon, contentDescription?.let { stringResource(it) }) },
            text = { Text(stringResource(titleRes)) }
        )
    }

    @Composable
    fun TabWithoutIcon(selected: Boolean, onClick: () -> Unit) {
        Tab(
            selected = selected,
            onClick = onClick,
            text = { Text(stringResource(titleRes)) }
        )
    }

    @Composable
    fun AdaptiveTab(windowSizeClass: WindowSizeClass, selected: Boolean, onClick: () -> Unit) {
        val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
        if (isCompact) {
            TabWithoutIcon(selected, onClick)
        } else {
            TabWithIcon(selected, onClick)
        }
    }
}

@Composable
fun AdaptiveTabRow(
    selectedTabIndex: Int,
    tabs: List<TabData>,
    onTabSelected: (Int) -> Unit
) {
    val windowSizeClass = calculateWindowSizeClass()

    if (tabs.size <= 4) {
        // Labels can be shown on a standard TabRow
        PrimaryTabRow(selectedTabIndex) {
            tabs.forEachIndexed { index, tab ->
                tab.AdaptiveTab(
                    windowSizeClass = windowSizeClass,
                    selected = selectedTabIndex == index,
                    onClick = { onTabSelected(index) },
                )
            }
        }
    } else {
        // For more than 4 tabs, medium and large screens have the same layout, but smaller screens allow scrolling tabs
        val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
        if (isCompact) {
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex
            ) {
                tabs.forEachIndexed { index, tab ->
                    tab.AdaptiveTab(
                        windowSizeClass = windowSizeClass,
                        selected = selectedTabIndex == index,
                        onClick = { onTabSelected(index) },
                    )
                }
            }
        } else {
            PrimaryTabRow(selectedTabIndex) {
                tabs.forEachIndexed { index, tab ->
                    tab.AdaptiveTab(
                        windowSizeClass = windowSizeClass,
                        selected = selectedTabIndex == index,
                        onClick = { onTabSelected(index) },
                    )
                }
            }
        }
    }
}
