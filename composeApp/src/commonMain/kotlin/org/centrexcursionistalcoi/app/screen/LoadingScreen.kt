package org.centrexcursionistalcoi.app.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.centrexcursionistalcoi.app.composition.LocalNavController
import org.centrexcursionistalcoi.app.platform.ui.PlatformLoadingIndicator
import org.centrexcursionistalcoi.app.route.Loading
import org.centrexcursionistalcoi.app.viewmodel.LoadingViewModel

object LoadingScreen : Screen<Loading, LoadingViewModel>(vmConstructor = ::LoadingViewModel) {
    @Composable
    override fun Content(viewModel: LoadingViewModel) {
        val navController = LocalNavController.current

        LaunchedEffect(viewModel) {
            viewModel.load(navController)
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            PlatformLoadingIndicator()
        }
    }
}
