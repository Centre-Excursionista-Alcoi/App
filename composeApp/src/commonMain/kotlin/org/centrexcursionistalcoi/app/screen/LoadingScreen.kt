package org.centrexcursionistalcoi.app.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import org.centrexcursionistalcoi.app.viewmodel.LoadingViewModel

@Composable
fun LoadingScreen(
    onLoggedIn: (name: String, groups: List<String>) -> Unit,
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
        LoadingBox(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        )
    }
}
