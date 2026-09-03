package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import org.centrexcursionistalcoi.app.auth.AuthBackend
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.viewmodel.LogoutViewModel
import org.koin.compose.koinInject

@Composable
fun LogoutScreen(
    afterLogout: () -> Unit
) {
    val authBackend = koinInject<AuthBackend>()
    viewModel { LogoutViewModel(authBackend, afterLogout) }

    LoadingBox()
}
