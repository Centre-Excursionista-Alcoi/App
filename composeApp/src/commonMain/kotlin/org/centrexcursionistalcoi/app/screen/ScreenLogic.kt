package org.centrexcursionistalcoi.app.screen

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import org.centrexcursionistalcoi.app.route.Route

inline fun <reified R : Route, reified VM: ViewModel> NavGraphBuilder.composable(screen: Screen<R, VM>) {
    composable<R> {
        screen.Content()
    }
}

@Composable
inline fun <reified VM: ViewModel> Screen<*, VM>.Content() {
    val viewModel = viewModel { vmConstructor() }

    Content(viewModel)
}
