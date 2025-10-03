package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.viewmodel.LoadingViewModel

@Composable
fun LoadingScreen(
    onLoggedIn: () -> Unit,
    onNotLoggedIn: () -> Unit,
    model: LoadingViewModel = viewModel { LoadingViewModel() }
) {
    LaunchedEffect(Unit) {
        model.load(
            onLoggedIn = onLoggedIn,
            onNotLoggedIn = onNotLoggedIn,
        )
    }

    Scaffold { paddingValues ->
        LoadingBox(paddingValues)
    }
}
