package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.viewmodel.LogoutViewModel

@Composable
fun LogoutScreen(
    afterLogout: () -> Unit
) {
    viewModel { LogoutViewModel(afterLogout) }

    LoadingBox()
}
