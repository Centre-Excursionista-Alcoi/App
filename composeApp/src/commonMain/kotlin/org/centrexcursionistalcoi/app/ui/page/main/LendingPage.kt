package org.centrexcursionistalcoi.app.ui.page.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.ui.screen.LendingDetailsScreen_Content
import org.centrexcursionistalcoi.app.ui.screen.LendingsActionBarIcons

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LendingPage(
    windowSizeClass: WindowSizeClass,
    lending: ReferencedLending,
    lendings: List<ReferencedLending>?,
    onCancelLendingRequest: (ReferencedLending) -> Unit,
    onLendingHistoryRequest: () -> Unit,
    onMemoryEditorRequested: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) {
            TopAppBar(
                title = { /* nothing, just buttons */ },
                actions = {
                    LendingsActionBarIcons(
                        lending,
                        lendings,
                        { onCancelLendingRequest(lending) },
                        onLendingHistoryRequest,
                    )
                },
            )
        }
        LendingDetailsScreen_Content(
            lending = lending,
            modifier = Modifier.fillMaxSize(),
            onMemoryEditorRequest = onMemoryEditorRequested,
        )
    }
}
