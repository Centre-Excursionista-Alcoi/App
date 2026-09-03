package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.runtime.Composable
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.viewmodel.LogoutViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun LogoutScreen(
    afterLogout: () -> Unit
) {
    koinViewModel<LogoutViewModel> { parametersOf(afterLogout) }

    LoadingBox()
}
