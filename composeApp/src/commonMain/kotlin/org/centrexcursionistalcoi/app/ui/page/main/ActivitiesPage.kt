package org.centrexcursionistalcoi.app.ui.page.main

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.activities_activities
import cea_app.composeapp.generated.resources.activities_memories
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.ui.page.main.activities.MemoriesPage
import org.jetbrains.compose.resources.stringResource

@Composable
fun ColumnScope.ActivitiesPage() {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { 2 }

    SecondaryTabRow(
        selectedTabIndex = pagerState.targetPage,
    ) {
        Tab(
            selected = pagerState.targetPage == 0,
            onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
            text = { Text(stringResource(Res.string.activities_memories)) }
        )
        Tab(
            selected = pagerState.targetPage == 1,
            onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
            text = { Text(stringResource(Res.string.activities_activities)) }
        )
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth().weight(1f),
    ) { page ->
        when (page) {
            0 -> MemoriesPage()
        }
    }
}
