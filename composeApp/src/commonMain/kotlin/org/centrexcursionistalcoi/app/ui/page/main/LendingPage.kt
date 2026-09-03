package org.centrexcursionistalcoi.app.ui.page.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.ui.screen.LendingDetailsScreen_Content
import org.centrexcursionistalcoi.app.ui.screen.LendingsActionBarIcons
import org.centrexcursionistalcoi.app.viewmodel.LendingPageModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
fun LendingPage(
    onCancelLendingRequest: (ReferencedLending) -> Unit,
    onLendingHistoryRequest: () -> Unit,
    onMemoryEditorRequested: (ReferencedLending) -> Unit,
    model: LendingPageModel = koinViewModel(),
) {
    val windowSizeClass = calculateWindowSizeClass()

    val lending by model.activeLending.collectAsState()
    val lendings by model.lendings.collectAsState()
    val activeLending = lending
    if (activeLending == null) {
        LoadingBox()
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) {
            TopAppBar(
                title = { /* nothing, just buttons */ },
                actions = {
                    LendingsActionBarIcons(
                        activeLending,
                        // Filter only the lendings owned by the logged in user
                        lendings?.filter { it.user.sub == activeLending.user.sub },
                        { onCancelLendingRequest(activeLending) },
                        onLendingHistoryRequest,
                    )
                },
            )
        }
        LendingDetailsScreen_Content(
            lending = activeLending,
            modifier = Modifier.fillMaxSize(),
            onMemoryEditorRequest = { onMemoryEditorRequested(activeLending) },
        )
    }
}
