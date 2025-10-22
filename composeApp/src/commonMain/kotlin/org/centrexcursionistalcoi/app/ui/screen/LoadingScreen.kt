package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.viewmodel.LoadingViewModel

@Composable
fun LoadingScreen(
    onLoggedIn: () -> Unit,
    onNotLoggedIn: () -> Unit
) {
    val vm = viewModel { LoadingViewModel(onLoggedIn, onNotLoggedIn) }

    val progress by vm.progress.collectAsState()

    Scaffold { paddingValues ->
        LoadingBox(paddingValues, progress)
    }
}
