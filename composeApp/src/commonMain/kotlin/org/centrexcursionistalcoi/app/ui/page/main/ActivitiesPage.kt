package org.centrexcursionistalcoi.app.ui.page.main

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.activities_activities
import cea_app.composeapp.generated.resources.activities_memories
import org.centrexcursionistalcoi.app.data.ReferencedMemory
import org.centrexcursionistalcoi.app.ui.page.main.activities.MemoriesPage
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.jetbrains.compose.resources.stringResource

@Composable
fun ColumnScope.ActivitiesPage(
    isAdmin: Boolean,
    memories: List<ReferencedMemory>?
) {
    if (memories == null) {
        LoadingBox()
    } else {
        var selectedTabIndex by remember { mutableStateOf(0) }

        SecondaryTabRow(
            selectedTabIndex = selectedTabIndex
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text(stringResource(Res.string.activities_memories)) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text(stringResource(Res.string.activities_activities)) }
            )
        }

        HorizontalPager(
            state = rememberPagerState { 2 },
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) { page ->
            when (page) {
                0 -> MemoriesPage(isAdmin, memories)
            }
        }
    }
}
