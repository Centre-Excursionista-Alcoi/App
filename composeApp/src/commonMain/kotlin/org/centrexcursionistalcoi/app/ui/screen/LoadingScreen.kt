package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.viewmodel.LoadingViewModel

@Composable
fun LoadingScreen(
    onLoggedIn: () -> Unit,
    onNotLoggedIn: () -> Unit
) {
    viewModel { LoadingViewModel(onLoggedIn, onNotLoggedIn) }

    Scaffold { paddingValues ->
        LoadingBox(paddingValues)
    }
}
