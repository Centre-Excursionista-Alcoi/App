package org.centrexcursionistalcoi.app.ui.reusable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.centrexcursionistalcoi.app.ui.platform.calculateWindowSizeClass
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSharedTransitionApi::class)
data class TabData(
    val title: String,
    val icon: ImageVector? = null,
    val filledIcon: ImageVector? = icon,
    val contentDescription: String? = title,
) {
    companion object {
        @Composable
        fun fromResources(
            titleRes: StringResource,
            icon: ImageVector? = null,
            filledIcon: ImageVector? = icon,
            contentDescription: StringResource? = titleRes,
        ) = TabData(
            title = stringResource(titleRes),
            icon = icon,
            filledIcon = filledIcon,
            contentDescription = contentDescription?.let { stringResource(it) },
        )
    }

    @Composable
    private fun TabWithIcon(selected: Boolean, onClick: () -> Unit) {
        Tab(
            selected = selected,
            onClick = onClick,
            icon = {
                AnimatedContent(selected) {
                    Icon(
                        imageVector = if (it) filledIcon!! else icon!!,
                        contentDescription = contentDescription
                    )
                }
            },
            text = { Text(title) }
        )
    }

    @Composable
    private fun TabWithoutIcon(selected: Boolean, onClick: () -> Unit) {
        Tab(
            selected = selected,
            onClick = onClick,
            text = { Text(title) }
        )
    }

    @Composable
    fun AdaptiveTab(windowSizeClass: WindowSizeClass, selected: Boolean, onClick: () -> Unit) {
        val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
        if (isCompact || icon == null) {
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
