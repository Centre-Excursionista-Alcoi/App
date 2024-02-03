package ui.reusable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.fade
import com.eygraber.compose.placeholder.material3.placeholder
import kotlinx.coroutines.launch
import ui.reusable.navigation.ScaffoldPage

@Composable
@ExperimentalFoundationApi
@ExperimentalMaterial3WindowSizeClassApi
fun AdaptiveScaffold(
    pages: List<ScaffoldPage>,
    floatingActionButton: @Composable () -> Unit = {},
    loadingItems: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { pages.size }

    val windowSizeClass = calculateWindowSizeClass()

    Scaffold(
        topBar = {
            if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                NavigationBar {
                    for ((index, page) in pages.withIndex()) {
                        NavigationBarItem(
                            selected = pagerState.currentPage == index,
                            icon = {
                                Icon(
                                    imageVector = page.icon,
                                    contentDescription = page.contentDescription,
                                    modifier = Modifier
                                        .placeholder(loadingItems, highlight = PlaceholderHighlight.fade())
                                )
                            },
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            label = {
                                Text(
                                    text = page.label(),
                                    modifier = Modifier
                                        .placeholder(loadingItems, highlight = PlaceholderHighlight.fade())
                                )
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = floatingActionButton
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    for ((index, page) in pages.withIndex()) {
                        NavigationRailItem(
                            selected = pagerState.currentPage == index,
                            icon = {
                                Icon(
                                    imageVector = page.icon,
                                    contentDescription = page.contentDescription,
                                    modifier = Modifier
                                        .placeholder(loadingItems, highlight = PlaceholderHighlight.fade())
                                )
                            },
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            label = {
                                Text(
                                    text = page.label(),
                                    modifier = Modifier
                                        .placeholder(loadingItems, highlight = PlaceholderHighlight.fade())
                                )
                            },
                            alwaysShowLabel = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
                        )
                    }
                }
            }
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact,
                modifier = Modifier.fillMaxSize()
            ) { index ->
                val page = pages[index]
                with(page) { PageContent() }
            }
        }
    }
}
