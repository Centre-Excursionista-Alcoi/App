package org.centrexcursionistalcoi.app.screen

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import org.centrexcursionistalcoi.app.route.Route

abstract class Screen<R: Route, VM: ViewModel>(
    val vmConstructor: () -> VM
) {
    @Composable
    abstract fun Content(viewModel: VM)
}
